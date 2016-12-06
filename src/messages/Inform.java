package messages;

import jade.util.leap.Serializable;

public class Inform implements Serializable{

	private static final long serialVersionUID = 1L;
	private double content;
	
	public Inform(double content) {
		this.content = content;
	}
	
	public double getContent() {
		return content;
	}
	
	public void setContent(double content) {
		this.content = content;
	}
}
