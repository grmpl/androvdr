package de.androvdr;


public interface IFileLogger {
	public String getLogFileName();
	public void setLogFileName(String logFileName);
	public int getLogLevel();
	public void setLogLevel(int loglevel);
	public void initLogFile(String logFileName, boolean append);
}
