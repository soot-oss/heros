package de.ishuo.sourcepainter;

public class InvalidOffsetException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4883502277579265891L;

	@Override
	public String getMessage() {
		return "Offset is invalid";
	}

}
