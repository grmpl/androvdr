package de.androvdr.svdrp;


import java.io.IOException;
import java.util.TimerTask;

import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Connection;
import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.androvdr.Preferences;
import de.androvdr.Recording;
import de.androvdr.devices.VdrDevice;



public class VDRConnection {
    
    private static transient Logger logger = LoggerFactory.getLogger(VDRConnection.class.getName());

    private static Connection connection;
    private static Integer syncer = 0;
    
    /**
     * If set, the connection will be kept open for some time,
     * so that consecutive request will be much faster
     */
    public static boolean persistentConnection = true;
    
    private static java.util.Timer timer;
    
    private static long lastTransmissionTime = 0;
    
    /**
     * The time in ms, the connection will be kept alive after
     * the last request. {@link #persistentConnection} has to be
     * set to true.
     */
    private static final int CONNECTION_KEEP_ALIVE = 15000;
    
    /**
     * Sends a SVDRP command to VDR and returns a response object, which represents the vdr response
     * @param cmd The SVDRP command to send
     * @return The SVDRP response
     */
	public synchronized static Response send(final Command cmd) {
		VdrDevice vdr = Preferences.getVdr();
		if (vdr == null)
			return new ConnectionProblem("No VDR defined");
		
		String host;
		int port, timeout;
		if (Preferences.useInternet) {
			host = "localhost";
			port = vdr.remote_local_port;
			timeout = vdr.remote_timeout;
		} else {
			host = vdr.getIP();
			port = vdr.getPort();
			timeout = vdr.timeout;
		}
		
		Response res = null;
		try {
			
			/*
			 *  prevent ConnectionCloser from closing the connection
			 */
			synchronized (syncer) {
				if (connection == null) {
					logger.debug("New connection to {} port {}", host, port);
					connection = new Connection(host, port, 1000, timeout, vdr.characterset, false);
					Version v = Connection.getVersion();
					Recording.VDRVersion = v.getMajor() * 10000 + v.getMinor() * 100 + v.getRevision();
				} else {
					logger.trace("old connection");
					lastTransmissionTime = System.currentTimeMillis();
				}
				logger.trace("--> {}", cmd.getCommand());
			}

			res = connection.send(cmd);
			lastTransmissionTime = System.currentTimeMillis();
			if (!persistentConnection) {
				connection.close();
				connection = null;
			} else {
				if (timer == null) {
					logger.trace("Starting connection closer");
					timer = new java.util.Timer("SVDRP connection closer");
					timer.schedule(new ConnectionCloser(), 0, 1000);
				}
			}
			logger.trace("<-- {}", res.getMessage());
		} catch (Exception e1) {
			close();
			res = new ConnectionProblem(e1.getMessage());
			logger.error(res.getMessage(), e1);
		}

		return res;
	}
    
    static class ConnectionCloser extends TimerTask {
        @Override
        public void run() {
        	synchronized (syncer) {
                if (connection != null && (System.currentTimeMillis() - lastTransmissionTime) > CONNECTION_KEEP_ALIVE) {
                    close();
                }
			}
        }
    }
    
    public synchronized static void close() {
        try {
        	if (connection != null) {
                logger.debug("Closing connection");
        		connection.close();
        	}
		} catch (IOException e) {
			logger.error("Couldn't close connection", e);
		} finally {
	        if(timer != null) {
	            logger.trace("Canceling ConnectionCloser");
	            timer.cancel();
	        }
			connection = null;
			timer = null;
	     
		}
    }
}