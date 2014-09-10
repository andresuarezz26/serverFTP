package control;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;

public class ServerDTP
{
	// Directorio raíz
	private String currentPath;

	// Socket de la conexión de datos
	ServerSocket socketDatos;

	public ServerDTP(ServerSocket socketDatos)
	{
		currentPath = System.getProperty("user.dir") + "/root";

		this.socketDatos = socketDatos;
	}

	public String getCurrentPath()
	{
		return currentPath;
	}

	public void setCurrentPath(String rootPath)
	{
		this.currentPath = rootPath;
	}

	public void receiveFile(InputStream is, String path) throws Exception
	{
		// Obtener nombre del archivo
		String[] particion = path.split("/");
		String nombreArchivo = particion[particion.length - 1];

		int filesize = 6022386;
		int bytesRead;
		int current = 0;
		byte[] mybytearray = new byte[filesize];

		FileOutputStream fos = new FileOutputStream(getCurrentPath() + "/" + nombreArchivo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		bytesRead = is.read(mybytearray, 0, mybytearray.length);
		current = bytesRead;

		do
		{
			bytesRead = is.read(mybytearray, current, (mybytearray.length - current));
			if (bytesRead >= 0)
				current += bytesRead;
		} while (bytesRead > -1);

		bos.write(mybytearray, 0, current);
		bos.flush();
		bos.close();
	}
}
