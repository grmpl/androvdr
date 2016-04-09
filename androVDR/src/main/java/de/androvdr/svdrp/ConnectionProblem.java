package de.androvdr.svdrp;

import org.hampelratte.svdrp.Response;

public class ConnectionProblem extends Response {

    private static final long serialVersionUID = 1L;

    public ConnectionProblem(String message) {
        super(503, (message == null) ? "Couldn't connect to VDR" : message);
        
    	if (this.message.toLowerCase().contains("access denied"))
    		this.message += " Check svdrphosts.conf.";
    }
    
    @Override
    public String toString() {
        return getMessage();
    }

}
