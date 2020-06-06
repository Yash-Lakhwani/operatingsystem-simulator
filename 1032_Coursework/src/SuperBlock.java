class SuperBlock {
	// number of blocks in the file system.
	int size;
	//public int size;
	// number of index blocks in the file system. 
	int iSize;
	//public int iSize;
	// first block of the free list
	int freeList;

	public String toString () {
		return
			"SUPERBLOCK:\n"
			+ "Size: " + size
			+ "  Isize: " + iSize
			+ "  freeList: " + freeList;
	}
}
