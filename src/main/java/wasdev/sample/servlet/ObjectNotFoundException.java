package wasdev.sample.servlet;


public class ObjectNotFoundException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String _bucket;
	private final String _object;

	public ObjectNotFoundException(String bucketName, String objectName) {
		_bucket = bucketName;
		_object = objectName;
	}
	
	@Override
	public String toString() {
		return String.format("object %s under bucker %s not found", _object, _bucket);
	}
}
