package ACLMsgContentObjects.CANCEL;

import jade.util.leap.Serializable;

public class Cancel implements Serializable{
	
	private static final long serialVersionUID = 1L;

	public Cancel() {
		System.out.println("This is a cancel message");
	}
}
