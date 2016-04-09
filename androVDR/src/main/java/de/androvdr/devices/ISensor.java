package de.androvdr.devices;

import java.util.ArrayList;

public interface ISensor extends IDevice {

	public ArrayList<String> getSensors();
	public String read(String command);

}
