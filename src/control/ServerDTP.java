package control;

public class ServerDTP
{
	// Directorio raíz
	private String currentPath;

	public ServerDTP()
	{
		currentPath = System.getProperty("user.dir") + "/root";
	}

	public String getCurrentPath()
	{
		return currentPath;
	}

	public void setCurrentPath(String rootPath)
	{
		this.currentPath = rootPath;
	}

}
