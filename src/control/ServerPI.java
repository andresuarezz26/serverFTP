package control;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;

import utilities.MyLogger;

public class ServerPI extends Thread
{
	private File apuntador;
	private boolean isRNFR;

	// Socket y flujo de escritura
	private Socket sktControl;
	private PrintWriter out;

	// Server DTP
	ServerDTP serverDTP;

	// Respuestas
	private final String SUCCESS = "S";
	private final String ERROR = "E";

	/**
	 * Constructor del Server PI
	 * 
	 * @param clientSocket
	 *            Socket de control hacia el cliente
	 * @param serverDTP
	 *            Componente que interactúa con el file system del servidor y se
	 *            encarga de la conexión de datos
	 */
	public ServerPI(Socket clientSocket, ServerDTP serverDTP)
	{

		try
		{
			MyLogger.setup();
		} catch (IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Problems with creating the log files");
		}

		try
		{
			this.sktControl = clientSocket;
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			// Iniciar el Server DTP
			this.serverDTP = serverDTP;
			apuntador = null;
			isRNFR = false;
		} catch (IOException e)
		{
			System.out.println("Error creando el flujo de transmisión");
		}

	}

	public void run()
	{
		MyLogger.logger.log(Level.INFO, "Accede un cliente");

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
					MyLogger.logger.log(Level.INFO, comando);

					String[] separacion = comando.split(" ");
					// 1. Comandos sin parámetros
					if (separacion.length == 1)
					{
						// Cerrar la conexión
						if (comando.equalsIgnoreCase("END"))
						{
							out.close();
							MyLogger.logger.log(Level.INFO, "Termina el cliente");

							// Comando desconocido
						} else if (comando.equalsIgnoreCase("LIST"))
						{
							String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
							out.println(respuestaList);

						} else
						{
							out.println(ERROR);
							MyLogger.logger.log(Level.INFO, "Comando desconocido");

						}

					}
					// 2. Comandos con un parámetro
					else if (separacion.length == 2)
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

						} else if (separacion[0].equalsIgnoreCase("STOR"))
						{
							commandSTOR(separacion[1]);

						} else if (separacion[0].equalsIgnoreCase("RETR"))
						{
							commandRETR(separacion[1]);
						}
						// Comando DELE
						else if (separacion[0].equalsIgnoreCase("DELE"))
						{
							commandDELE(separacion[1]);

						} else if (separacion[0].equalsIgnoreCase("RNFR"))
						{
							commandRNFR(separacion[1]);
						} else if (separacion[0].equalsIgnoreCase("RNTO"))
						{
							commandRNTO(separacion[1]);
						}

					}
					// 3. Comandos con más de un parámetro
					else
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

			MyLogger.logger.log(Level.INFO, sb.toString());

			return sb.toString();

		} else
		{
			MyLogger.logger.log(Level.WARNING, "El parámetro no es un directorio");

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
			MyLogger.logger.log(Level.INFO, "Se cambia de directorio");

		} else
		{
			File f = new File(serverDTP.getCurrentPath() + "/" + rutaRelativa);
			if (f.exists())
			{

				if (f.isDirectory())
				{
					serverDTP.setCurrentPath(serverDTP.getCurrentPath() + "/" + rutaRelativa);
					out.println(SUCCESS);
					MyLogger.logger.log(Level.INFO, "Se cambia de directorio");

				} else
				{
					out.println(ERROR);
					MyLogger.logger.log(Level.WARNING, "La carpeta no existe");

				}

			} else
			{
				out.println(ERROR);
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");

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
				MyLogger.logger.log(Level.INFO, resultado);

			} else
			{
				String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
				out.println(respuestaList);
				MyLogger.logger.log(Level.INFO, respuestaList);

			}

		} else
		{
			out.println(ERROR);
			MyLogger.logger.log(Level.WARNING, "El archivo no existe");

		}

	}

	/**
	 * Atienda la solicitud de almacenar el archivo enviado por el cliente
	 * 
	 * @param archivo
	 *            Archivo que se va a almacenar en el servidor
	 */
	public void commandSTOR(String rutaArchivo)
	{
		try
		{
			Socket sktDatos = serverDTP.socketDatos.accept();
			InputStream is = sktDatos.getInputStream();
			serverDTP.receiveFile(is, rutaArchivo);
			System.out.println("Se recibieron los datos del cliente");
			MyLogger.logger.log(Level.INFO, "Se recibieron los datos del cliente");
			out.println(SUCCESS);
		} catch (Exception e)
		{
			out.println("ERROR");
			MyLogger.logger.log(Level.WARNING, "El archivo no se pudo almacenar");

		}
	}

	/**
	 * Atender la solicitud de envíar un archivo al cliente
	 * 
	 * @param nombreArchivo
	 */
	public void commandRETR(String nombreArchivo)
	{
		try
		{
			File f = new File(serverDTP.getCurrentPath() + "/" + nombreArchivo);
			if (f.exists() && f.isFile())
			{
				out.println("successful_retr");
				serverDTP.sendFile(nombreArchivo);
				System.out.println("Se envian los datos al cliente");
				MyLogger.logger.log(Level.INFO, "Se envían los datos al cliente");

			} else
			{
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");
				System.out.println("El archivo no existe");
				out.println("error_retr");

			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "No se pudo enviar el archivo");
			out.println(ERROR);
		}
	}

	/**
	 * 
	 * @param nombreArchivo
	 */
	public void commandDELE(String nombreArchivo)
	{
		try
		{
			File f = new File(serverDTP.getCurrentPath() + "/" + nombreArchivo);
			if (f.exists() && f.isFile())
			{
				if (f.delete())
				{
					MyLogger.logger.log(Level.INFO, "Se elimina el archivo");

					out.println("successful_dele");
				} else
				{
					MyLogger.logger.log(Level.WARNING, "No se puede eliminar el archivo");

					out.println("error_dele");
				}

			} else
			{
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");

				System.out.println("El archivo no existe");
				out.println("error_dele");
			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "Hubo problemas en el flujo de la información");

			out.println(ERROR);
		}

	}

	public void commandRNTO(String nuevoNombre)
	{
		try
		{
			if (apuntador.exists() && apuntador.isFile() && (apuntador != null) && isRNFR)
			{
				File nuevoArchivo = new File(serverDTP.getCurrentPath() + "/" + nuevoNombre);
				if (apuntador.renameTo(nuevoArchivo))
				{
					MyLogger.logger.log(Level.INFO, "Se cambia el nombre del archivo");
					out.println("successful_rename");
					isRNFR = false;

				} else
				{
					MyLogger.logger.log(Level.WARNING, "No se pudo renombrar");
					out.println("error_rename");
					isRNFR = false;
				}
			} else
			{
				isRNFR = false;
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");
				out.println("error_dele");
			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "No se pudo cambiar el nombre");
			isRNFR = false;
			out.println(ERROR);
		}
	}

	public void commandRNFR(String nombreArchivo)
	{
		try
		{
			File f = new File(serverDTP.getCurrentPath() + "/" + nombreArchivo);
			if (f.exists() && f.isFile())
			{
				isRNFR = true;
				apuntador = f;
				MyLogger.logger.log(Level.WARNING, "Se selecciona el archivo correctamente");
				out.println("successful_rnfr");
			} else
			{
				isRNFR = false;
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");
				System.out.println("El archivo no existe");
				out.println("error_rnfr");
				apuntador = null;
			}

		} catch (Exception e)
		{
			isRNFR = false;
			MyLogger.logger.log(Level.WARNING, "No se pudo seleccionar el archivo");

			out.println(ERROR);
			apuntador = null;
		}
	}
}
