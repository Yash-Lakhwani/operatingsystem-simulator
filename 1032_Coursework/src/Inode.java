class Inode {
	public final static int SIZE = 64;	// size in bytes
	int flags;
	int owner;
	int fileSize;
	int pointer[] = new int[13];

	public String toString() {
		String s = "[Flags: " + flags
		+ "  Size: " + fileSize + "  ";
		for (int i = 0; i < 13; i++) 
			s += "|" + pointer[i];
		return s + "]";
	}
}