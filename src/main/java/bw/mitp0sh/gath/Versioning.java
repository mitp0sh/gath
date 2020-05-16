package bw.mitp0sh.gath;

public class Versioning {
	protected static final short MAJOR_VERSION = 0;
	protected static final short MINOR_VERSION = 0;
	protected static final int PATCH_VERSION = 0;
	protected static final String TYPE = "alpha"; // allowed: alpha, beta, rc or empty
	
	@Override
	public String toString() {
		return MAJOR_VERSION + "." + MINOR_VERSION + "." + PATCH_VERSION + (TYPE.length() != 0 ? "-" + TYPE : "");
	}
}
