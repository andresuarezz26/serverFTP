package control;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

public class ServerPI extends Thread
{

	// Socket y flujo de escritura
	private Socket sktControl;
	private PrintWriter out;

	// Server DTP
	ServerDTP serverDTP;

	// Respuestas
	private final String SUCCESS = "S";
	private final String ERROR = "E";

	public ServerPI(Socket clientSocket)
	{
		try
		{
			this.sktControl = clientSocket;
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			serverDTP = new ServerDTP();
		} catch (IOException e)
		{
			System.out.println("Error creando el flujo de transmisión");
		}

	}

	public void run()
	{
		System.out.println("Got a client");
		while (true)
		{
			if (sktControl.isClosed())
			{
			} else
			{
				try
				{

					BufferedReader in = new BufferedReader(new InputStreamReader(sktControl.getInputStream()));
					// Mientras el buffer está vacío
					while (!in.ready())
					{
					}

					String comando = in.readLine();

					String[] separacion = comando.split(" ");
					// 1. Comandos sin parámetros
					if (separacion.length == 1)
					{
						// Cerrar la conexión
						if (comando.equalsIgnoreCase("END"))
						{
							out.close();
							// Comando desconocido
						} else if (comando.equalsIgnoreCase("LIST"))
						{
							String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
							out.println(respuestaList);

						} else
						{
							out.println(ERROR);
						}
						// 2. Comandos con un parámetro
					} else if (separacion.length == 2)
					{
						// Comando CWD
						if (separacion[0].equalsIgnoreCase("CWD"))
						{
							commandCWD(separacion[1]);

						}
						// Comando LIST
						else if (separacion[0].equalsIgnoreCase("LIST"))
						{
							commandLIST(separacion[1]);
						}
						// 3. Comandos con más de un parámetro
					} else
					{
						out.println(ERROR);

					}

				} catch (IOException e)
				{
					System.out.println("Error en el flujo de información");
				}
			}

		}

	}

	/**
	 * Obtiene la lista de archivos y directorios de un directorio
	 * 
	 * @param path
	 *            Ruta del directorio
	 * @return Lista de archivos y directorios separados por un ;
	 */
	public String getListOfDirectory(String path)
	{
		File f = new File(serverDTP.getCurrentPath());
		if (f.isDirectory())
		{
			String[] listOfFiles = f.list();
			StringBuilder sb = new StringBuilder();
			for (String file : listOfFiles)
			{
				sb.append(file + ";");
			}
			return sb.toString();

		} else
		{
			return ERROR;
		}

	}

	/**
	 * Lógica del comando CWD
	 * 
	 * @param rutaRelativa
	 *            Ruta relativa del directorio al cual se quiere cambiar
	 */
	public void commandCWD(String rutaRelativa)
	{
		if (rutaRelativa.equals("/"))
		{
			serverDTP.setCurrentPath(System.getProperty("user.dir") + "/root");
			out.println(SUCCESS);

		} else
		{
			File f = new File(serverDTP.getCurrentPath() + "/" + rutaRelativa);
			if (f.exists())
			{

				if (f.isDirectory())
				{
					serverDTP.setCurrentPath(serverDTP.getCurrentPath() + "/" + rutaRelativa);
					out.println(SUCCESS);

				} else
				{
					out.println(ERROR);
				}

			} else
			{
				out.println(ERROR);
			}
		}
	}

	/**
	 * Listar los archivos de un directorio o las características de un archivo
	 * 
	 * @param ruta
	 *            Ruta del directorio o archivo
	 */
	public void commandLIST(String ruta)
	{

		File f = new File(serverDTP.getCurrentPath() + "/" + ruta);
		if (f.exists())
		{
			if (f.isFile())
			{
				Date fechaModificacion = new Date(f.lastModified());
				String resultado = "Tamaño: " + f.length() + " bytes;" + "Oculto: " + f.isHidden() + ";" + "Última modificación: " + fechaModificacion.toString();
				out.println(resultado);
			} else
			{
				String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
				out.println(respuestaList);
			}

		}

	}
}
