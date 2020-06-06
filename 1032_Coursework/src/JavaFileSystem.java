
import java.util.*;
import java.io.*;

class JavaFileSystem implements FileSystem {
	private static final boolean DELETE_DISK_ON_SHUTDOWN = false;
	
    // Set up the constants for the whence field in seek
    public static final int SEEK_SET    = 0;
    public static final int SEEK_CUR    = 1;
    public static final int SEEK_END    = 2;
    
    // Disk
    private Disk disk;
    private SuperBlock sb;

    // Free list cache - first block of the list
    private IndirectBlock freeList;

    // File Table
    private FileTable ft;

    JavaFileSystem() {
    		// Create the disk
        disk = new Disk();
        
        // Read the super-block (in the case the disk is already formatted)
    		sb = new SuperBlock();
    		try {
    			disk.read(0, sb);
    		}
    		catch (Exception e) {
    			// nothing to do, sb is already set to (0, 0, 0) by disk.read()
    		}
    		
    		// Create the file table
    		ft = new FileTable();

    		// Empty the free list cache
    		freeList = null;

    }


    /**
     * Initialize the the disk to a state representing an empty file-system.
     * Fill in the super block, mark all inodes as "unused", and link
     * all data blocks into the free list.
     *
     * @param size the size of the disk in blocks
     * @param isize the number of inode blocks
     * @return 0 if successful, -1 if not
     */
    public int formatDisk(int size, int iSize) {
    		// validate input
    	
	    	if (iSize < 1) {
	    	    error("The number of inode blocks is too small");
	    	    return -1;
	    	}
	    	
	    	if (size < (2 * iSize + 2)) {
	    	    error("The size of the disk is too small");
	    	    return -1;
	    	}
	    	
	    	
	    	
	    	// Create an empty iNode block
	    	InodeBlock ib = new InodeBlock();
	    	for (int i = 0; i < ib.node.length; i++) {
	    		// mark the inode as unused
	    	    ib.node[i].flags = 0;
	    	}
	    	
	    	// Write empty inode blocks	
	    	for (int i = 1; i <= iSize; i++) {
	    	    disk.write(i, ib);
	    	}


	    	// Build the list of free blocks (fill them bottom-up)

	    	// dataBlocks = the total number of data blocks
	    	// offset = the offset within the indirect block being filled
	    	// lastFreeListBlock = the last block of free list filled

	    	int dataBlocks = size - iSize - 1;
	    	int offset = Disk.BLOCK_SIZE / 4 - 1;
	    	int lastFreeListBlock = 0;

	    	IndirectBlock B = new IndirectBlock();

	    	for (int i = size - 1; i >= iSize + 1; i--) {

	    	    if ((offset == 0) || (i == iSize + 1)) {
		    		// Write the block to the disk and update the reference
		    		// to the last block filled
		    		B.pointer[0] = lastFreeListBlock;
	
		    		disk.write(i, B);
	
		    		// cache the last free block in the list
		    		lastFreeListBlock = i;
	
		    		// Prepare the next block if this is not the last one
	
		    		if (i > iSize + 1) {
		    		    offset = Disk.BLOCK_SIZE / 4 - 1;
		    		    B.clear();
		    		}
	    	    }
	    	    else {
	    	    		// Add this block to the free list
	    	    		B.pointer[offset--] = i;
	    	    }
	    	}
	    	
	    	// Initialize the superblock
	    	sb.size = size;
	    	sb.iSize = iSize;
	    	sb.freeList = iSize + 1;

	    	// write the superblock to disk
	    	disk.write(0, sb);

	    	// Cache the first block on the free list
	    	freeList = B;
	    	
	    	return 0;
    } // formatDisk

    /**
     * Close all files and shut down the simulated disk.
     */
    public int shutdown() {
	    	// Close open files
	    	for (int i = 0; i < FileTable.MAX_FILES; i++) {
	    	    if (ft.isValid(i)) close(i);
	    	}
	
	    	// Synchronize the free list
	    	if ((freeList != null) && (sb.freeList > 0)) {
	    	    disk.write(sb.freeList, freeList);
	    	}
	
	    	// Synchronize the super block
	    	disk.write(0, sb);
	
	    	// Stop the disk (keep the file by default)
	    	disk.stop(DELETE_DISK_ON_SHUTDOWN);
	
	    	return 0;

    } // shutdown

    /**
     * Create a new (empty) file and return a file descriptor.
     * Then, open the file.
     *
     * @return the file descriptor
     */
    public int create() {
	    	// Make sure there is a free entry in the file table
	
	    	int fd = ft.allocate();
	    	if (fd < 0) {
	    	    error("Too many files are open");
	    	    return -1;
	    	}
	
	    	ft.free(fd);
	
	    	// Build inode
	
	    	Inode inode = new Inode();
	
	    	inode.flags = 1;
	    	inode.owner = 0;
	    	inode.fileSize = 0;
	
	    	for (int i = 0; i < inode.pointer.length; i++) {
	    	    inode.pointer[i] = 0;
	    	}
	
	    	// Allocate the inode and write it to the disk
	
	    	int ind = allocateInode(inode);
	    	if (ind < 0) 
	    		return -1;  // the disk is full
	
	    	// Open the file
	
	    	return open(ind, inode);

    } // create

    /**
     * Return the inumber of an open file
     *
     * @param fd the file descriptor of the file
     * @return the inode number of the file
     */
    public int inumber(int fd) {
	    	// Check input
	
	    	if (!ft.isValid(fd)) {
	    	    error("The given file descriptor does not point to an open file.");
	    	    return -1;
	    	}
	
	    	// Return the inumber
	
	    	return ft.getInumber(fd);
    }

    /**
     * Open an existing file identified by its inumber
     *
     * @param iNumber the inode number of the file
     * @return the file descriptor number
     */
    public int open(int iNumber) {
	    	// Check the input
	    	
	    	if ((iNumber <= 0) || (iNumber > sb.iSize * (Disk.BLOCK_SIZE/Inode.SIZE))) {
	    	    error("Inode number out of range");
	    	    return -1;
	    	}
	    	
	    	// Read the inode
	
	    	Inode inode = readInode(iNumber);
	    	if (inode == null) 
	    		return -1;  // error occured
	
	    	// Open the file
	
	    	return open(iNumber, inode);
    } // open

 

    /**
     * Read up to buffer.length bytes from the open file indicated by fd,
     * starting at the current seek pointer, and update the seek pointer.
     * Return the number of bytes read, which may be less than buffer.length
     * if the seek pointer is near the current end of the file.
     * In particular, return 0 if the seek pointer is greater than or
     * equal to the size of the file.
     *
     * @param fd the file descriptor
     * @param buffer a pre-initialized reading buffer
     * @return number of bytes read, or -1 on error
     */

    public int read(int fd, byte[] buffer) {
	    	// Check input
	
	    	if (!ft.isValid(fd)) {
	    	    error("The given file descriptor does not point to an open file.");
	    	    return -1;
	    	}
	
	    	// Get the value of the seek pointer
	
	    	int sp = ft.getSeekPointer(fd);
	
	    	// Ignore empty buffers
	
	    	if (buffer.length <= 0) return 0;
	
	    	// Get the inode
	
	    	Inode I = ft.getInode(fd);
	
	    	// Determine the starting point of the read
	
	    	// block = logical block number
	    	// offset = offset within the block
	    	// bp = buffer position (same as the number of bytes read)
	
	    	int block  = sp / Disk.BLOCK_SIZE;
	    	int offset = sp % Disk.BLOCK_SIZE;
	
	    	int bp = 0;
	
	    	byte[] readBytes = new byte[Disk.BLOCK_SIZE];
	    	
	    	// Do the read

	    	while ((bp < buffer.length) && (sp < I.fileSize)) {

	    	    // Determine the disk number of the block

	    	    int disk_block = getBlock(I, block);
	    	    if (disk_block < 0) return bp;    // error occured

	    	    // Determine the number of bytes to read

	    	    int readSize = buffer.length - bp;
	    	    if ((offset + readSize) > Disk.BLOCK_SIZE) readSize = Disk.BLOCK_SIZE - offset;
	    	    if ((sp + readSize) > I.fileSize) readSize = I.fileSize - sp;

	    	    // Read (disk_block == 0 means the hole)

	    	    if (disk_block == 0) {
		    		for (int i = offset; i < offset + readSize; i++) {
		    		    buffer[bp++] = 0;
		    		}
	
		    		//debug("Reding a hole");
	    	    }
	    	    else {

		    		//debug("Reading block " + disk_block + ", logical number = " + block);
	
		    		disk.read(disk_block, readBytes);
	
		    		for (int i = offset; i < offset + readSize; i++) {
		    		    buffer[bp++] = readBytes[i];
		    		}
	    	    }

	    	    // Increment position on disk

	    	    offset += readSize;
	    	    if (offset >= Disk.BLOCK_SIZE) {
		    		offset = 0;
		    		block++;
	    	    }

	    	    sp += readSize;
	    	    ft.setSeekPointer(fd, sp);
	    	}

	    	return bp;

    } // read

    /**
     * Transfer buffer.length bytes from the buffer to the file, starting
     * at the current seek pointer, and add buffer.length to the seek pointer.
     *
     * @param fd the file descriptor
     * @param buffer the buffer to write
     * @return number of bytes written if success, -1 if error
     */
    public int write(int fd, byte[] buffer) {
	    	// Check input
	
	    	if (!ft.isValid(fd)) {
	    	    error("The given file descriptor does not point to an open file.");
	    	    return -1;
	    	}
	    	
	    	// Ignore empty buffers
	    	if (buffer.length <= 0) return 0;
	    	
		// Get the value of the seek pointer
	    	int sp = ft.getSeekPointer(fd);
	    	
	    	// Get the inode
	    	Inode I = ft.getInode(fd);

	    	// Determine the starting point of the write

	    	// block = logical block number
	    	// offset = offset within the block
	    	// bp = buffer position (same as the number of bytes written)

	    	int block  = sp / Disk.BLOCK_SIZE;
	    	int offset = sp % Disk.BLOCK_SIZE;

	    	int bp = 0;

	    	// Do the write

	    	while (bp < buffer.length) {

	    	    // Determine the disk number of the block, and allocate if needed

	    	    int disk_block = getBlock(I, block);
	    	    
	    	    // Deal with holes

	    	    boolean justAllocated = false;

	    	    if (disk_block == 0) {
		    	    	disk_block = allocateBlock(fd, block);
		    		justAllocated = true;
		    		
		    		if (disk_block < 0) 
		    			return bp;
	    	    }
	    	    
	    	    // Deal with errors

	    	    if (disk_block <= 0) {
		    		// Error occured
		    		error("Unable to translate the block address to disk address");
		    		error("  seek pointer: " + sp + " = (" + block + ", " + offset + ")");
		    		return -1;
	    	    }

	    	    // Determine the number of bytes to write

	    	    int writeSize = buffer.length - bp;
	    	    if ((offset + writeSize) > Disk.BLOCK_SIZE) writeSize = Disk.BLOCK_SIZE - offset;

	    	    // Determine whether we will finish writing beyond current end of file

	    	    boolean writingBeyondEOF = writeSize + sp > I.fileSize;

	    	    // Determine whether we need to do read before write

	    	    boolean needToRead = true;

	    	    if ((offset == 0) && (writeSize >= Disk.BLOCK_SIZE)) {

		    		// We are writing the whole block
	
		    		needToRead = false;
	    	    }
	    	    else if ((offset == 0) && (writingBeyondEOF)) {
	    		
		    		// We are writing beyond the end of the file, starting at block offset 0
	
		    		needToRead = false;
	    	    }
	    	    else if (justAllocated) {

		    		// We have just allocated the block
	
		    		needToRead = false;
	    	    }

	    	    // Prepare data to write

	    	    byte[] writeBytes = new byte[Disk.BLOCK_SIZE];

	    	    if (justAllocated) for (int i = 0; i < Disk.BLOCK_SIZE; i++) writeBytes[i] = 0;

	    	    if (needToRead) disk.read(disk_block, writeBytes);
	    	    for (int i = offset; i < offset + writeSize; i++) writeBytes[i] = buffer[bp++];

	    	    // Write

	    	    disk.write(disk_block, writeBytes);

	    	    //debug("Writing block " + disk_block + ", logical number = " + block);

	    	    // Increment position on disk

	    	    offset += writeSize;
	    	    if (offset >= Disk.BLOCK_SIZE) {
		    		offset = 0;
		    		block++;
	    	    }

	    	    sp += writeSize;
	    	    ft.setSeekPointer(fd, sp);

	    	    // Update the size of the file, if needed

	    	    if (writingBeyondEOF) {
		    		I.fileSize = block * Disk.BLOCK_SIZE + offset;	 
	    	    }
	    	    
	    	}

	System.out.println("SB = " + sb);
	    	
    		return bp;
    } // write

    /**
     * Update the seek pointer by offset, according to whence.
     * Return the new value of the seek pointer.
     * If the new seek pointer would be negative, leave it unchanged
     * and return -1.
     *
     * @param fd file descriptor
     * @param offset the offset of the seek pointer
     * @param whence either SEEK_SET, SEEK_CUR, or SEEK_END
     * @return the new value of the seek pointer, or -1 on error
     */
    public int seek(int fd, int offset, int whence) {
	    	// Check input
	
	    	if (!ft.isValid(fd)) {
	    	    error("The given file descriptor does not point to an open file.");
	    	    return -1;
	    	}
	
	    	// Calculate the new seek pointer
	
	    	int p = -1;
	
	    	int oldSeek = ft.getSeekPointer(fd);
	    	int fileSize = ft.getInode(fd).fileSize;
	
	    	switch (whence) {
	    	case SEEK_SET: p = offset; break;
	    	case SEEK_CUR: p = oldSeek + offset; break;
	    	case SEEK_END: p = fileSize + offset; break;
	    	default:
	    	    error("Invalid seek whence constant.");
	    	    return -1;
	    	}
	
	    	if (p < 0) {
	    	    error("Negative seek pointer.");
	    	    return -1;
	    	}
	
	    	// Set the seek pointer
	
	    	if (ft.setSeekPointer(fd, p) < 0) 
	    		return -1;
	    	
	    	return p;


    } // seek

    // Write the inode back to disk and free the file table entry
    public int close(int fd) {
    		// Check input
    	
	    	if (!ft.isValid(fd)) {
	    	    error("The given file descriptor does not point to an open file.");
	    	    return -1;
	    	}
	    	

	    	// Retrieve the inumber and the inode

	    	int inum = ft.getInumber(fd);
	    	Inode inode = ft.getInode(fd);

	    	// Free the file descriptor

	    	ft.free(fd);

	    	// Update the inode

	    	return writeInode(inum, inode);
    } // close

    // Delete the file with the given inumber, freeing all of its blocks.
    public int delete(int iNumber) {
	    	// Check if the file is already open
	
	    	if (ft.getFDfromInumber(iNumber) >= 0) {
	    	    error("Cannot delete an open file");
	    	    return -1;
	    	}
	            
	    	// Read the inode
	
	    	Inode I = readInode(iNumber);
	    	if (I == null) return -1;  // error occured
	
	    	// Check the inode
	
	    	if (I.flags == 0) {
	    	    error("File not found");
	    	    return -1;
	    	}
	
	    	// Figure the size of the file in blocks
	
	    	int size = (I.fileSize + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE;
	
	    	// De-allocate all data blocks
	
	    	// count = the number of data blocks freed
	
	    	int count = 0;
	
	    	// Level 0
	
	    	for (int i = 0; i < 10; i++) {
	    	    if ((i < size) && (I.pointer[i] > 0)) {
	    		freeBlock(I.pointer[i]);
	    		count++;
	    	    }
	    	}
	
	    	//debug("Level 0: " + count + " data blocks freed");
	
	    	// Level 1
	
	    	count += freeIndirect(I.pointer[10], 1);
	
	    	//debug("Levels 0, 1: " + count + " data blocks freed");
	
	    	// Level 2
	
	    	count += freeIndirect(I.pointer[11], 2);
	
	    	//debug("Levels 0, 1, 2: " + count + " data blocks freed");
	
	    	// Level 3
	
	    	count += freeIndirect(I.pointer[12], 3);
	
	    	//debug("" + count + " data blocks freed");
	
	    	// Remove the inode
	
	    	I.flags = 0;
	
	    	return writeInode(iNumber, I);
	       
    } // delete

    public String toString() {
        throw new RuntimeException("not implemented");
    }
    
    // private helper utilities
    private void error(String msg) {
	    	System.out.flush();
	    	System.err.flush();
	    	System.out.println("[ERROR] " + msg);
	    	System.out.flush();
    }
    
    /**
     * Find and allocate first free inode.
     *
     * @return the index of free inode or -1 if full
     */
    private int allocateInode(Inode inode) {
	
		InodeBlock ib = new InodeBlock();
	
		// Traverse the inode section of the disk
	
		for (int i = 1; i <= sb.iSize; i++) {
	
			// read an inode block
		    disk.read(i, ib);
	
		    // Traverse through the inode block
	
		    for (int j = 0; j < ib.node.length; j++) {
				if (ib.node[j].flags == 0) {
				    // We have just found a free inode entry
		
				    ib.node[j] = inode;
				    disk.write(i, ib);
		
				    return inodeNumber(i, j); 
				}
		    }
		}
	
		error("The disk contains too many files");
	
		return -1;
    }
    
    /**
     * Encode an inode number
     *
     * @param iblock the block number
     * @param ioffset the inode offset
     * @return encoded absolute inode number
     */
    private int inodeNumber(int iblock, int ioffset) {
    		return (iblock - 1) * (Disk.BLOCK_SIZE / Inode.SIZE) + ioffset + 1;
    }

    /**
     * Open a file
     *
     * @param inum the inode number
     * @param inode the inode
     * @return the file descriptor number
     */
    private int open(int inum, Inode inode) {

		// Check the inode
	
		if (inode.flags == 0) {
		    error("File not found");
		    return -1;
		}
	
		// Check if the file is already open
	
		if (ft.getFDfromInumber(inum) >= 0) {
		    error("The file is already open");
		    return -1;
		}
	
		// Allocate a file descriptor
	
		int fd = ft.allocate();
		if ((fd < 0) || (fd >= FileTable.MAX_FILES)) return -1;
	
		// Add information
	
		int status = ft.add(inode, inum, fd);
		if (status < 0) {
		    error("Error occured while opening the file");
		    ft.free(fd);
		    return -1;
		}
	
		return fd;
    }

    /**
     * Read the inode corresponding to the inumber of a file.
     * 
     * @param inode the inode number of the file to be opened
     * @return The corresponding inode.
     */
    private Inode readInode(int inode) {
		// check the inumber is valid
    		if (inode <= 0) {
    			error("Illegal inode number");
    		    return null;
    		}
    		
    		// get the corresponding inode block
    		int block = iBlock(inode);
    		
    		if ((block < 1) || (block > sb.iSize)) {
    		    error("Illegal inode number");
    		    return null;
    		}
    		
    		// Read the inode block
    		
    		InodeBlock ib = new InodeBlock();
    		disk.read(block, ib);
    		
    		// Now return the corresponding inode
    		return ib.node[ioffset(inode)];	
	}
    
    
    /**
     * Write an inode to the disk
     *
     * @param inum the inode number
     * @param inode the inode to write
     * @return 0 if success, -1 if error
     */
    private int writeInode(int inum, Inode inode) {
	    	// Check input
	
	    	if (inum <= 0) {
	    	    error("Illegal inode number");
	    	    return -1;
	    	}
	
	    	int block = iBlock(inum);
	
	    	if ((block < 1) || (block > sb.iSize)) {
	    	    error("Illegal inode number");
	    	    return -1;
	    	}
	
	    	// Read the inode block
	    	
	    	InodeBlock ib = new InodeBlock();
	    	disk.read(block, ib);
	    	
	    	// Update the inode
	
	    	ib.node[ioffset(inum)] = inode;
	    	disk.write(block, ib);
	
	    	return 0;  	
    }
    /**
     * Get the  block number of the corresponding inode.
     *
     * @param inode the inode number
     * @return the inode block number
     */
    private int iBlock(int inode) {
	    	return (inode - 1) / (Disk.BLOCK_SIZE / Inode.SIZE) + 1;
    }
    
    /**
     * Get the inode offset number
     *
     * @param inode the inode number
     * @return the inode offset within the block
     */
    private int ioffset(int inode) {
		return (inode - 1) % (Disk.BLOCK_SIZE / Inode.SIZE);
    }

    /**
     * Read the block number
     *
     * @param inode the inode
     * @param block the logical block number
     * @return the corresponding block number on the disk, 0 if not available, -1 on error
     */
    private int getBlock(Inode inode, int block) {

	// Figure out the size of file in blocks and validate the block number

	int size = (inode.fileSize + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE;

	if (block < 0) 
		return -1;
	if (block >= size) 
		return 0;

	// Determine the level of indirection and relative indices

	// N = pointers per indirect block
	// level = the level of indirection
	// p = index of the block relative to first block of same indirection level
	// i0 = index in the inode
	// i1, i2, i3 = indices in the first, second, and third indirect blocks

	// Please refer to the design document for details

	final int N = Disk.BLOCK_SIZE / 4;

	int level = 0;
	int p = 0;
	int i0 = 0;
	int i1 = 0;
	int i2 = 0;
	int i3 = 0;

	if (block <= 9) { // direct blocks
	    level = 0;
	    i0 = p = block;
	}
	else if (block <= (9 + N)) { // single indirect
	    level = 1;
	    p = block - 10;
	    i0 = 10;
	    i1 = p;
	}
	else if (block <= (9 + N + N * N)) { // double indirect
	    level = 2;
	    p = block - (10 + N);
	    i0 = 11;
	    i1 = p / N;
	    i2 = p % N;
	}
	else if (block <= (9 + N + N * N + N * N * N)) { // triple indirect
	    level = 3;
	    p = block - (10 + N + N * N);
	    i0 = 12;
	    i1 = p / (N * N);
	    i2 = (p / N) % N;
	    i3 = p % N;
	}
	else {
	    error("The file is too big");
	    return -1;
	}

	// Level 0

	if (level == 0) 
		return inode.pointer[i0];

	// Levels 1, 2, and 3

	// ib = indirect block

	IndirectBlock ib = new IndirectBlock();

	// Read first indirect block to ib

	// disk_i1 = block number of the first indirect block

	int disk_i1 = inode.pointer[i0];
	if (disk_i1 <= 0) 
		return -1; 
	else disk.read(disk_i1, ib);

	// Read first indirect block (Level 1 only)

	if (level == 1) 
		return ib.pointer[i1];

	// Read second indirect block to ib

	// disk_i2 = block number of the second indirect block

	int disk_i2 = ib.pointer[i1];
	if (disk_i2 <= 0) 
		return -1; 
	else 
		disk.read(disk_i2, ib);

	// Read second indirect block (Level 2 only)

	if (level == 2) 
		return ib.pointer[i2];

	// Read third indirect block to ib - Level 3 only

	// disk_i3 = block number of the second indirect block

	int disk_i3 = ib.pointer[i2];
	if (disk_i3 <= 0) 
		return -1; 
	else 
		disk.read(disk_i3, ib);

	// Read third indirect block

	return ib.pointer[i3];
    }

    /**
     * Allocate a block and assign it to an open file
     *
     * @param fd the file descriptor
     * @param where logical block number in the file
     * @return block number, or -1 on error
     */
    private int allocateBlock(int fd, int where) {

		// Check the input
	
		if (!ft.isValid(fd)) {
		    error("The given file descriptor does not point to an open file.");
		    return -1;
		}
	
		// Check free space
	
		if (sb.freeList <= 0) {
		    error("Disk full.");
		    return -1;
		}
	
		// Allocate a block
	
		int block = allocateBlock();
		if (block < 0) return -1;
	
		// Get the inode
		
		Inode I = ft.getInode(fd);
	
		// Add the block
	
		if (addBlock(I, block, where) < 0) {
		    freeBlock(block);
		    return -1;
		}
	
		return block;
    }

    /**
     * Allocate a block
     *
     * @return block number, or -1 on error
     */
    private int allocateBlock() {

		// Check free space
	
		if (sb.freeList <= 0) {
		    error("Disk full.");
		    return -1;
		}
	
		// Fill cache if empty
	
		if (freeList == null) {
	
		    freeList = new IndirectBlock();
	
		    disk.read(sb.freeList, freeList);
		}
		
		// Find first non-empty block on the list, or set offset to last
		// offset if the block is empty
	
		int offset;
	
		for (offset = 1; (offset < Disk.BLOCK_SIZE / 4 - 1) && (freeList.pointer[offset] <= 0);
		     offset++) ; 
	
		// Get the block number of the chosen block
	
		int freeBlock = freeList.pointer[offset];
	
		// Detach it
	
		freeList.pointer[offset] = 0;
	
		// Deal with the case the free list block is empty
	
		if (freeBlock == 0) {
	
		    freeBlock = sb.freeList;
	
		    // Detach it from the free list and empty the cache
	
		    sb.freeList = freeList.pointer[0];
	
		    offset = 0;
		    freeList = null;
		}
	
		//debug("Allocated block " + freeBlock + " from free list offset " + offset);
	
		return freeBlock;
    }
    
    /**
     * Free given block
     *
     * @param block the block number to free
     * @return 0 if success, or -1 on error
     */
    private int freeBlock(int block) {

		// Check input
	
		if (block <= 0) return -1;
	
		// Deal with the case there are no more free blocks left
	
		if  (sb.freeList <= 0) {
	
		    //debug("Creating the free list and attaching block " + block + " to it");
	
		    sb.freeList = block;
	
		    freeList = new IndirectBlock();
		    freeList.clear();
	
		    return 0;
		}
	
		// Fill the cache if empty
	
		if (freeList == null) {
	
		    freeList = new IndirectBlock();
	
		    disk.read(sb.freeList, freeList);
		}
		
		// Find last unused element of the list, or set offset = 0 if the block is full

		int offset;

		for (offset = Disk.BLOCK_SIZE / 4 - 1;
		    (offset > 0) && (freeList.pointer[offset] > 0);
		     offset--) ;

	
		// Add the block to the list
	
		if (offset <= 0) {
	
		    //debug("Attaching block " + block + " to the beginning if the free list");
		    
		    // Write the in-memory free list block to the disk
	
		    disk.write(sb.freeList, freeList);
	
		    // Add the current block to the beginning of the list
	
		    freeList = new IndirectBlock();
		    freeList.clear();
		    freeList.pointer[0] = sb.freeList;
	
		    sb.freeList = block;
		}
		else {
	
		    //debug("Adding block " + block + " to free list to offset " + offset);
	
		    freeList.pointer[offset] = block;
		}
		
		return 0;
    }
    
    /**
     * Add a block to the inode
     *
     * @param inode the inode
     * @param block the block number
     * @param where logical block number in the file
     * @return 0 if successful, or -1 if error
     */
    private int addBlock(Inode inode, int block, int where) {

		// Determine the level of indirection and relative indeces
	
		// N = pointers per indirect block
		// level = the level of indirection
		// p = index of the block relative to first block of same indirection level
		// i0 = index in the inode
		// i1, i2, i3 = indeces in the first, second, and third indirect blocks
	
		// Please refer to the design document for details
	
		final int N = Disk.BLOCK_SIZE / 4;
	
		int level = 0;
		int p = 0;
		int i0 = 0;
		int i1 = 0;
		int i2 = 0;
		int i3 = 0;
	
		if (where <= 9) {
		    level = 0;
		    i0 = p = where;
		}
		else if (where <= (9 + N)) {
		    level = 1;
		    p = where - 10;
		    i0 = 10;
		    i1 = p;
		}
		else if (where <= (9 + N + N * N)) {
		    level = 2;
		    p = where - (10 + N);
		    i0 = 11;
		    i1 = p / N;
		    i2 = p % N;
		}
		else if (where <= (9 + N + N * N + N * N * N)) {
		    level = 3;
		    p = where - (10 + N + N * N);
		    i0 = 12;
		    i1 = p / (N * N);
		    i2 = (p / N) % N;
		    i3 = p % N;
		}
		else {
		    error("The file is too big");
		    return -1;
		}
		
		//debug("Adding block " + block + " to level " + level + ": p = " + p + ", i0 = " + i0    + ", i1 = " + i1 + ", i2 = " + i2 + ", i3 = " + i3);
	 
		// Level 0
	
		if (level == 0) {
		    inode.pointer[i0] = block;
		    return 0;
		}
	
		// Levels 1, 2, and 3
	
		// allocated = number of allocated blocks
		// allocatedBocks = nmbers of allocated blocks
		// ib = indirect block
	
		int allocated = 0;
		int[] allocatedBlocks = new int[3];
		IndirectBlock ib = new IndirectBlock();
	
		// Read first indirect block to ib
	
		// disk_i1 = block number of the first indirect block
	
		int disk_i1 = inode.pointer[i0];
		if (disk_i1 <= 0) {
		    
		    // Allocate corresponding number of blocks
	
		    for (int i = 0; i < level; i++) {
	
			int b = allocateBlock();
	
			if (b <= 0) {
			    
			    // Error occured, do cleanup and exit
			    
			    for (int j = 0; j < i; j++) freeBlock(allocatedBlocks[j]);
			    return -1;
			}
	
			allocatedBlocks[i] = b;
			allocated++;
		    }
	
		    // Add the first one to the inode
	
		    disk_i1 = inode.pointer[i0] = allocatedBlocks[--allocated];
	
		    ib.clear();
		}
		else {
		    //debug("addBlock: reading i1 block " + disk_i1);
		    disk.read(disk_i1, ib);
		}
	
		// Update and write first indirect block (Level 1 only)
	
		if (level == 1) {
		    ib.pointer[i1] = block;
		    disk.write(disk_i1, ib);
		    return 0;
		}
	
		// Read second indirect block
	
		// disk_i2 = block number of the second indirect block
	
		boolean toBeAllocated = allocated > 0;
		int disk_i2 = (toBeAllocated) ? (allocatedBlocks[--allocated]) : (ib.pointer[i1]);
		if (toBeAllocated || (disk_i2 <= 0)) {
		    if (disk_i2 <= 0) {
			
			if (allocated > 0) {
			    error("Internal error");    // Should never happen
			    for (int j = 0; j < allocated; j++) freeBlock(allocatedBlocks[j]);
			    return -1;
			}
			
			// Allocate the corresponding number of blocks
			
			for (int i = 0; i < level - 1; i++) {
			    
			    int b = allocateBlock();
			    
			    if (b <= 0) {
				
				// Error occured, do cleanup and exit
				
				for (int j = 0; j < i; j++) freeBlock(allocatedBlocks[j]);
				return -1;
			    }
			    
			    allocatedBlocks[i] = b;
			    allocated++;
			}
	
			// Add the first one to the first indirect block
	
	 		disk_i2 = ib.pointer[i1] = allocatedBlocks[--allocated];
			disk.write(disk_i1, ib);
			
			ib.clear();
		    }
		    else {
	
			// It was already allocated
	
	 		ib.pointer[i1] = disk_i2;
			disk.write(disk_i1, ib);
			
			ib.clear();
		    }
		}
		else {
		    //debug("addBlock: reading i2 block " + disk_i2);
		    disk.read(disk_i2, ib);
		}
	
		// Update and write second indirect block (Level 2 only)
	
		if (level == 2) {
		    ib.pointer[i2] = block;
		    disk.write(disk_i2, ib);
		    return 0;
		}
	
		// Read third indirect block - Level 3 only
	
		// disk_i3 = block number of the second indirect block
	
		toBeAllocated = allocated > 0;
		int disk_i3 = (toBeAllocated) ? (allocatedBlocks[--allocated]) : (ib.pointer[i2]);
		if (toBeAllocated || (disk_i3 <= 0)) {
		    if (disk_i3 <= 0) {
	
			if (allocated > 0) {
			    error("Internal error");    // Should never happen
			    for (int j = 0; j < allocated; j++) freeBlock(allocatedBlocks[j]);
			    return -1;
			}
	
			// Allocate an indirect block
			
			int b = allocateBlock();
			
			if (b <= 0) {
			    // Error occured, do cleanup and exit
			    freeBlock(b);
			    return -1;
			}
			
			// Add it to the second indirect block
			
			disk_i3 = ib.pointer[i2] = b;
			disk.write(disk_i2, ib);
			
			ib.clear();
		    }
		    else {
	
			// It was already allocated
	
	 		ib.pointer[i2] = disk_i3;
			disk.write(disk_i2, ib);
			
			ib.clear();
		    }
		}
		else {
		    //debug("addBlock: reading i3 block " + disk_i3);
		    disk.read(disk_i3, ib);
		}
	
		// Update and write third indirect block
	
		ib.pointer[i3] = block;
		disk.write(disk_i3, ib);
	
		return 0;
    }


    /**
     * Free an indirect block
     *
     * @param block the block number to free
     * @param level the level of indirection (0 == not an indirect block)
     * @return number of data blocks freed if success, or -1 on error
     */
    private int freeIndirect(int block, int level) {

		// Check input
	
		if (block == 0) return  0;
		if (block <  0) return -1;
		if (level <  0) return -1;
	
		// Level 0 (just free the block)
	
		if (level == 0) {
		    return (freeBlock(block) < 0) ? (-1) : (1);
		}
	
		// Traverse through the list of blocks and free all of them
	
		// count = the number of blocks freed
		// ib = the indirect block
	
		int count = 0;
		IndirectBlock ib = new IndirectBlock();
	
		// Read the block
		
		disk.read(block, ib);
	
		// Traverse it
	
		for (int i = 0; i < Disk.BLOCK_SIZE / 4; i++) {
	
		    if (ib.pointer[i] <= 0) continue;
	
		    // Free the block
	
		    int r = freeIndirect(ib.pointer[i], level - 1);
	
		    // Update count
	
		    if (r > 0) {
			count += r;
		    }
		}
	
		freeBlock(block);
	
		return count;
    }



}
