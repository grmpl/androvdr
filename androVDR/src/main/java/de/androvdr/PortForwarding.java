/*
 * www.jcraft.com/jsch/
 * 
 * Copyright (c) 2002,2003,2004,2005,2006,2007,2008 Atsuhiko Yamanaka, 
 * JCraft,Inc. All rights reserved. Redistribution and use in source and binary 
 * forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 3. The names of the authors may not be used to endorse or promote products 
 * derived from this software without specific prior written permission. THIS 
 * SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * JCRAFT,INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package de.androvdr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import de.androvdr.activities.AndroVDR;
import de.androvdr.devices.VdrDevice;

public class PortForwarding implements Runnable {
	private static transient Logger logger = LoggerFactory.getLogger(PortForwarding.class);
	
	// wird fuer die Kommunikation mit der GUI gebraucht
	public static volatile boolean guiFlag = false; // wird beim GUI-Dialogende auf true gesetzt
	public static String sshPassword;
	public static String sshPassphrase;
	public static String guiMessage;
	public static boolean positiveButton;  // ist nach Dialogende true,wenn der positiveButton gedrueckt wurde
	
	private static Session session = null;
	
	static final int START_PROGRESS_DIALOG = 20 , STOP_PROGRESS_DIALOG = 10 ,
	                 PROMPT_PASSWORD = 0 , PROMPT_YES_NO = 1 , PROMPT_MESSAGE = 2,
	                 PROMPT_PASSPHRASE = 3;
	

	// Handler zum Benachrichtigen der GUI, wird beim Instanzieren gesetzt
	private Handler handler;
	
		
	// der Context der aufrufenden Klasse
	private static Activity sActivity;
	
	
	@SuppressWarnings("static-access")
	public PortForwarding(Handler h,Activity activity){
		synchronized (Preferences.useInternetSync) {
			Preferences.useInternet = false;
			Preferences.useInternetSync.notify();
		}
		session = null;
		guiFlag = false;
		positiveButton = false;
		this.handler = h;
		this.sActivity = activity;
		handler.sendEmptyMessage(START_PROGRESS_DIALOG);
		Thread thread = new Thread(this);
		thread.start();
	}
	
	// dieser Tread etabliert PortForwarding
	public void run() {
		try {
			JSch.setLogger(new MyLogger());
			JSch jsch = new JSch();

			File knownHosts = new File(Preferences.getSSHKnownHostsFileName());
			knownHosts.createNewFile();
			jsch.setKnownHosts(knownHosts.getAbsolutePath());

			VdrDevice vdr = Preferences.getVdr();
			if (vdr.sshkey != null) {
				File keyfile = new File(Preferences.getSSHKeyFileName());
				BufferedWriter out = new BufferedWriter(new FileWriter(keyfile));
				out.write(vdr.sshkey);
				out.close();
				jsch.addIdentity(keyfile.getAbsolutePath());
				keyfile.delete();
			}

			session = jsch.getSession(vdr.remote_user, vdr.remote_host,	vdr.remote_port);
			
			Properties config = new Properties();
			config.put("compression.s2c", "zlib@openssh.com,zlib,none");
			config.put("compression.c2s", "zlib@openssh.com,zlib,none");
			session.setConfig(config);
			
			// password will be given via UserInfo interface.
			UserInfo ui = new MyUserInfo();
			session.setUserInfo(ui);
			session.connect();
			logger.trace("SSH-Session verbunden");

			int assinged_port = session.setPortForwardingL(vdr.remote_local_port, "localhost", vdr.getPort());
			logger.debug("localhost:{} -> {}",  assinged_port, (vdr.getIP() + ":" + vdr.getPort()));

			if (vdr.extremux) {
				assinged_port = session.setPortForwardingL(vdr.remote_streaming_port, "localhost", vdr.streamingport);
				logger.debug("localhost:{} -> {}",  assinged_port, (vdr.getIP() + ":" + vdr.streamingport));
			}
			
			if (vdr.vdradmin) {
				assinged_port = session.setPortForwardingL(vdr.remote_vdradmin_port, "localhost", vdr.vdradmin_port);
				logger.debug("localhost:{} -> {}",  assinged_port, (vdr.getIP() + ":" + vdr.vdradmin_port));
			}

			handler.sendEmptyMessage(STOP_PROGRESS_DIALOG); // alles OK, beende
															// mit dieser
															// Nachricht die
															// Fortschrittsanzeige
			logger.trace("PortForwarding eingerichtet");
			
			synchronized (Preferences.useInternetSync) {
				Preferences.useInternet = true;
				Preferences.useInternetSync.notify();
			}
		} catch (Exception e) {
			logger.error("Couldn't establish connection", e);
			guiMessage = sActivity.getString(R.string.portforwarding_fails)	+ e.toString();
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptMessage() auf
			handler.sendEmptyMessage(PROMPT_MESSAGE);
			// warte, bis GUI fertig ist
			while (guiFlag == false)
				;
		}
	}	
	
	public void disconnect(){
		if(session != null){
			if(session.isConnected()){
				session.disconnect();
				logger.debug("Session getrennt");
			}
		}
		session = null;
		
		synchronized (Preferences.useInternetSync) {
			Preferences.useInternet = false;
			Preferences.useInternetSync.notify();
		}
	}
	
	// wird vom Handler der aufrufenden GUI-Klasse benutzt
    public static void sshDialogHandlerMessage(Message msg) {

    	switch(msg.what){
    	case PROMPT_PASSWORD:
    		promptPassword(sActivity);
    		break;
    	case PROMPT_PASSPHRASE:
    		promptPassphrase(sActivity);
    		break;
    	case PROMPT_YES_NO:
    		promptYesNo(sActivity);
    		break;
    	case PROMPT_MESSAGE:// Fehler beim Aktivieren von PortForwarding (Aufruf im Portforwarding-Thread)
    		promptMessage(sActivity);
      		//break; Kein break,hier. ProgressDialog auch beenden
    	case STOP_PROGRESS_DIALOG:// ProgressDialog beenden, Portforwarding ist aktiviert !
    		if(progressDialog != null)
				try {
					progressDialog.dismiss();
				} catch (IllegalArgumentException e) {
					logger.error("progressDialog dismiss", e);
				}
    		break;
    	case START_PROGRESS_DIALOG:
    		//progressDialog = ProgressDialog.show(c, "", c.getString(R.string.starte_portforwarding),true,false);
    		
    		progressDialog = new ProgressDialog(sActivity);
    		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		progressDialog.setMessage(sActivity.getString(R.string.start_portforwarding));
    		progressDialog.setCancelable(true);
    		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Abbrechen", progressAbbruchListener);
    		progressDialog.show();
    		
    	}
    }

    // hier wird Portforwarding waehrend der Progressanzeige durch den User abgebrochen
    static OnClickListener progressAbbruchListener = new OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if(session != null){
				if(session.isConnected()){
					session.disconnect();
					logger.debug("Session durch Benutzer abgebrochen");
				}
			}
			
			synchronized (Preferences.useInternetSync) {
				Preferences.useInternet = false;
				Preferences.useInternetSync.notify();
			}
			
			AndroVDR.portForwarding = null;
		}
    };
	
	// installiert Callback-Funktionen zur Interaktion mit dem GUI
	private class MyUserInfo implements UserInfo, UIKeyboardInteractive{
		
		@Override
		public String getPassphrase(){
			logger.trace("getPassphrase");
			return sshPassphrase; 
		}
		
		@Override
		public boolean promptPassphrase(String arg0){
			logger.trace("promptPassphrase");
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptPassword() auf
			handler.sendEmptyMessage(PROMPT_PASSPHRASE);
			// warte, bis Passwort eingegeben ist
	    	while(guiFlag == false);

			return positiveButton;  //true;
		}
		

		@Override
		public String getPassword(){ // liefert das Passwort zurueck
			return sshPassword; 
		}

		@Override
		public boolean promptPassword(String arg0) { // Aufruf GUI zum Eingeben des Passwortes
			logger.trace("promptPassword");
			
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptPassword() auf
			handler.sendEmptyMessage(PROMPT_PASSWORD);
			// warte, bis Passwort eingegeben ist
	    	while(guiFlag == false);

			return positiveButton;  //true;
		}

		@Override
		public boolean promptYesNo(String s) {  // zeigt Host-Fingerprint an
			guiMessage = s;
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptYesNo() auf
			handler.sendEmptyMessage(PROMPT_YES_NO);
			// warte, bis Fingerprint bestaetigt ist
	    	while(guiFlag == false);
			
			logger.debug("promptYesNo: {}", s);
			
			return positiveButton;  //true;
		}

		@Override
		public void showMessage(String s) { // unbenutzt
			guiMessage = s;
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptMessage() auf
			handler.sendEmptyMessage(PROMPT_MESSAGE);
			// warte, bis showMessage bestaetigt ist
	    	while(guiFlag == false);
			logger.debug("showMessage: {}", s);
		}

		@Override   // unbenutzt
		public String[] promptKeyboardInteractive(String destination,String name,
                String instruction,String[] prompt,boolean[] echo) {
			StringBuffer s = new StringBuffer();
			logger.trace("promptKey... Name = {}", name);
			s.append(name+"\n");
			logger.trace("promptKey... Instruction = {}", instruction);
			s.append(instruction+"\n");
			for(int i=0;i < prompt.length;i++){
				logger.trace("promptKey... {} - {}",prompt[i], echo[i]);
				s.append(prompt[i]+"-"+echo[i]+"\n");
			}
			guiMessage = s.toString();
			positiveButton = false;
			guiFlag = false;
			// rufe GUI-Dialog promptYesNo() auf
			handler.sendEmptyMessage(PROMPT_YES_NO);
			// warte, bis sendEmptyMessage bestaetigt ist
	    	while(guiFlag == false);
	    	
			return null;
		}
		
	}
	
	// ****************************************************************************
	// hier die GUI-Dialoge
	// ****************************************************************************
	
	private static ProgressDialog progressDialog;
	
	static void promptPassword(Activity activity){
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);  
		alert.setTitle(activity.getString(R.string.pw_msg));  
		final EditText input = new EditText(activity);
		input.setTransformationMethod(PasswordTransformationMethod.getInstance());
		alert.setView(input);  
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   sshPassword = input.getText().toString(); 
 			   positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   });  
 	   alert.setNegativeButton(activity.getString(R.string.break_msg), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   // Canceled. 
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
 		   }  
 	   });  
 	   if (! activity.isFinishing())
 		   alert.show();  
	}
	
	static void promptPassphrase(Activity activity){
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);  
		alert.setTitle(activity.getString(R.string.pw_msg_passphrase));  
		final EditText input = new EditText(activity);
		input.setTransformationMethod(PasswordTransformationMethod.getInstance());
		alert.setView(input);  
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   sshPassphrase = input.getText().toString(); 
 			   positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   });  
 	   alert.setNegativeButton(activity.getString(R.string.break_msg), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   // Canceled. 
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
 		   }  
 	   });  
 	   if (! activity.isFinishing())
 		   alert.show();  
	}
	
	static void promptYesNo(Activity activity){
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);
		alert.setTitle(activity.getString(R.string.warning));  
		alert.setMessage(guiMessage);  
		alert.setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   });  
 	   alert.setNegativeButton(activity.getString(R.string.no), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   // Canceled. 
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
 		   }  
 	   });  
 	   if (! activity.isFinishing())
 		   alert.show();  
	}
	
	static void promptMessage(Activity activity){
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);
		alert.setTitle(activity.getString(R.string.message));  
		alert.setMessage(guiMessage);  
		alert.setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			  positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   }); 
	   if (! activity.isFinishing())
		   alert.show();  
	}
	
	public static class MyLogger implements com.jcraft.jsch.Logger {

		public boolean isEnabled(int level) {
			return true;
		}

		public void log(int level, String message) {
			switch (level) {
			case DEBUG:
				logger.debug(message);
				break;
			case INFO:
				logger.info(message);
				break;
			case WARN:
				logger.warn(message);
				break;
			case ERROR:
			case FATAL:
				logger.error(message);
				break;
			}
		}
	}
}
