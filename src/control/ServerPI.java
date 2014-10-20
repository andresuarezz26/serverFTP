package control;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
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
	// Archivo al que se apunta para cambiar el nombre
	private File apuntador;

	// Socket y flujo de escritura
	private Socket sktControl;
	private PrintWriter out;

	// Server DTP
	ServerDTP serverDTP;

	// Respuestas
	private final String SUCCESS = "S";
	private final String ERROR = "E";

	// Atributos para la autenticación
	private String usuarioEsperandoAutenticacion = null;
	private String passwordUsuario = null;
	boolean esperandoUsuario = false;
	private int contadorAutenticacion = 0;
	boolean logueado = false;
	boolean isAscii = true;

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

					System.out.println(comando);
					MyLogger.logger.log(Level.INFO, comando);
					String[] separacion = comando.split(" ");

					// Si el usuario se quiere loguear y ya está logueado
					if (logueado == true && separacion[0].equalsIgnoreCase("USER"))
					{
						out.println("already_logged");
					} else
					{
						// Si el usuario no se ha logueado y quiere usar los
						// comandos
						if (logueado == false && !(separacion[0].equalsIgnoreCase("USER") || separacion[0].equalsIgnoreCase("PASS")))
						{
							out.println("please_log");
						} else
						{

							// 1. Comandos sin parametros
							if (separacion.length == 1)
							{
								// Cerrar la conexión
								if (comando.equalsIgnoreCase("END"))
								{
									out.close();
									MyLogger.logger.log(Level.INFO, "Termina el cliente");
									// Comando desconocido
								} else if (comando.equalsIgnoreCase("LIST") || comando.equalsIgnoreCase("LS"))
								{

									String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
									out.println(respuestaList);

								} else if (comando.equalsIgnoreCase("ASCII"))
								{

									isAscii = true;
									out.println("ASCII_mode");

								} else if (comando.equalsIgnoreCase("BINARY"))
								{

									isAscii = false;
									out.println("binary_mode");

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
								if (separacion[0].equalsIgnoreCase("CWD") || separacion[0].equalsIgnoreCase("CD"))
								{
									commandCWD(separacion[1]);

								}
								// Comando LIST
								else if (separacion[0].equalsIgnoreCase("LIST") || separacion[0].equalsIgnoreCase("LS"))
								{
									commandLIST(separacion[1]);
								} else if (separacion[0].equalsIgnoreCase("STOR") || separacion[0].equalsIgnoreCase("PUT"))
								{
									commandSTOR(separacion[1]);
								} else if (separacion[0].equalsIgnoreCase("RETR") || (separacion[0].equalsIgnoreCase("GET")))
								{
									commandRETR(separacion[1]);
								}
								// Comando DELE
								else if (separacion[0].equalsIgnoreCase("DELE") || separacion[0].equalsIgnoreCase("DELETE"))
								{
									commandDELE(separacion[1]);
								} else if (separacion[0].equalsIgnoreCase("RNFR") || separacion[0].equalsIgnoreCase("SELECT"))
								{
									commandRNFR(separacion[1]);

								} else if (separacion[0].equalsIgnoreCase("RNTO") || separacion[0].equalsIgnoreCase("RENAME"))
								{
									commandRNTO(separacion[1]);
								} else if (separacion[0].equalsIgnoreCase("USER"))
								{
									usuarioEsperandoAutenticacion = separacion[1];
									esperandoUsuario = true;
									contadorAutenticacion = 0;
									commandUSER();

								} else if (separacion[0].equalsIgnoreCase("PASS"))
								{
									if (contadorAutenticacion == 1)
									{
										if (esperandoUsuario == true)
										{
											passwordUsuario = separacion[1];
											esperandoUsuario = false;
											if (commandPASS(usuarioEsperandoAutenticacion, passwordUsuario) == false)
											{
												passwordUsuario = null;
												usuarioEsperandoAutenticacion = null;

											} else
											{
												logueado = true;
											}
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
							// 3. Comandos con más de un parámetro
							else
							{
								out.println(ERROR);

							}

						}

					}
				} catch (IOException e)
				{
					System.out.println("Error en el flujo de información");
				}
			}

			contadorAutenticacion++;
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

	public void commandUSER()
	{
		out.println("waiting_pass");
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
				MyLogger.logger.log(Level.INFO, "Se envían los datos al cliente");
			} else
			{
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");

				out.println("error_retr");

			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "No se pudo enviar el archivo");
			out.println(ERROR);
		}
	}

	/**
	 * Permite eliminar un archivo
	 * 
	 * @param nombreArchivo
	 *            Nombre del archivo que se va a eliminar
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
				out.println("error_dele");
			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "Hubo problemas en el flujo de la información");

			out.println(ERROR);
		}

	}

	/**
	 * Permite cambiar el nombre a un archivo
	 * 
	 * @param nuevoNombre
	 *            String nuevo nombre
	 */
	public void commandRNTO(String nuevoNombre)
	{
		try
		{
			if (apuntador.exists() && apuntador.isFile() && (apuntador != null))
			{

				File nuevoArchivo = new File(serverDTP.getCurrentPath() + "/" + nuevoNombre);
				if (apuntador.renameTo(nuevoArchivo))
				{
					MyLogger.logger.log(Level.INFO, "Se cambia el nombre del archivo");
					out.println("successful_rename");

				} else
				{

					MyLogger.logger.log(Level.WARNING, "No se pudo renombrar");
					out.println("error_rename");
				}
			} else
			{
				MyLogger.logger.log(Level.WARNING, "El archivo no existe");
				out.println("error_rename");
			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "No se pudo cambiar el nombre");
			out.println(ERROR);
		}
	}

	/**
	 * Permite comprobar la identidad del usuario
	 * 
	 * @param nombreUsuario
	 *            nombre del usuario
	 * @param passwordUsuario
	 *            password del usuario
	 * @return True si corresponden ambos datos y falso en caso contrario
	 */
	public boolean commandPASS(String nombreUsuario, String passwordUsuario)
	{
		if (nombreUsuario == null || passwordUsuario == null)
		{
			out.println("autentication_error");

			return false;
		} else
		{

			try
			{
				// Leer el listado de Usuarios y contrasenas
				BufferedReader reader = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/root/" + "usersftp.txt"));

				String line = reader.readLine();
				boolean salir = false;
				while (line != null && salir == false)
				{

					line = reader.readLine();
					if (line != null)
					{
						String[] separacion = line.split(";");

						if (separacion[0].equalsIgnoreCase(usuarioEsperandoAutenticacion) && separacion[1].equalsIgnoreCase(passwordUsuario))
						{
							out.println("autentication_success");

							logueado = true;
							reader.close();
							return true;

						}
					}

				}
				out.println("autentication_error");
				reader.close();

			} catch (Exception e)
			{

				e.printStackTrace();
			}
			return false;

		}
	}

	/**
	 * Permite seleccionar el archivo al que se le va a cambiar el nombre
	 * 
	 * @param nombreArchivo
	 *            Nombre del archivo que se desea seleccionar
	 */
	public void commandRNFR(String nombreArchivo)
	{
		try
		{
			File f = new File(serverDTP.getCurrentPath() + "/" + nombreArchivo);
			if (f.exists() && f.isFile())
			{
				apuntador = f;
				MyLogger.logger.log(Level.WARNING, "Se selecciona el archivo correctamente");
				out.println("successful_rnfr");
			} else
			{

				MyLogger.logger.log(Level.WARNING, "El archivo no existe");

				out.println("error_rnfr");
				apuntador = null;
			}

		} catch (Exception e)
		{
			MyLogger.logger.log(Level.WARNING, "No se pudo seleccionar el archivo");

			out.println(ERROR);
			apuntador = null;
		}
	}
}
