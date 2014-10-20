package control;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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

	/**
	 * Permite obtener el archivo envíado por el cliente
	 * 
	 * @param is
	 *            Flujo por el cual se recibe el archivo
	 * @param path
	 *            Ruta del archivo del cliente
	 * @throws Exception
	 *             Excepción en caso de que haya problemas en el flujo
	 */
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

	/**
	 * Permite enviar un archivo al cliente
	 * 
	 * @param path
	 *            Ruta del archivo
	 */
	public void sendFile(String path)
	{

		try
		{

			File myFile = new File(getCurrentPath() + "/" + path);
			if (myFile.exists() && myFile.isFile())
			{
				Socket sktData = socketDatos.accept();
				OutputStream out = sktData.getOutputStream();

				byte[] mybytearray = new byte[(int) myFile.length() + 1];
				FileInputStream fis = new FileInputStream(myFile);
				BufferedInputStream bis = new BufferedInputStream(fis);
				bis.read(mybytearray, 0, mybytearray.length);
				System.out.println("Sending...");
				out.write(mybytearray, 0, mybytearray.length);

				out.flush();
				bis.close();
				out.close();
				sktData.close();
			} else
			{
				System.out.println("El archivo no existe");
			}

		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}
}
