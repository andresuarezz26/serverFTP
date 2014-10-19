package control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

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

	//Atributos para la autenticaci�n
	private String usuarioEsperandoAutenticacion=null;
	private String passwordUsuario= null;
	boolean esperandoUsuario = false;
	private int contadorAutenticacion=0;
	boolean logueado=false;
	boolean isAscii=true;

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
					System.out.println(comando);
					String[] separacion = comando.split(" ");
					//Si el usuario se quiere loguear y ya est� logueado
					if(logueado==true&&separacion[0].equalsIgnoreCase("USER")){
						out.println("already_logged");
					}else{
						//Si el usuario no se ha logueado y quiere usar los comandos
						if(logueado==false&&!(separacion[0].equalsIgnoreCase("USER")||separacion[0].equalsIgnoreCase("PASS"))){
							out.println("please_log");
						}else{

							// 1. Comandos sin parametros
							if (separacion.length == 1)
							{
								// Cerrar la conexión
								if (comando.equalsIgnoreCase("END"))
								{
									out.close();
									//Comando desconocido
								} else if (comando.equalsIgnoreCase("LIST"))
								{

									String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
									out.println(respuestaList);

								} else if(comando.equalsIgnoreCase("ascii"))
								{
	
									System.out.println("llego al ascii");
									isAscii=true;
									out.println("ASCII_mode");
									
								}else if(comando.equalsIgnoreCase("binary"))
								{

									System.out.println("llego al binario");
									isAscii=false;
									out.println("binary_mode");

								}else{

									out.println(ERROR);

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
								//Comando DELE
								else if(separacion[0].equalsIgnoreCase("DELE"))
								{
									commandDELE(separacion[1]);
								}else if(separacion[0].equalsIgnoreCase("RNFR"))
								{
									commandRNFR(separacion[1]);


								} else if(separacion[0].equalsIgnoreCase("RNTO") )
								{
									commandRNTO(separacion[1]);
								}else if(separacion[0].equalsIgnoreCase("USER") )
								{
									usuarioEsperandoAutenticacion=separacion[1];
									esperandoUsuario=true;
									contadorAutenticacion=0;
									commandUSER();

								}else if(separacion[0].equalsIgnoreCase("PASS")){
									if(contadorAutenticacion==1){
										if(esperandoUsuario==true)
										{
											passwordUsuario=separacion[1];
											esperandoUsuario=false;
											if(commandPASS(usuarioEsperandoAutenticacion,passwordUsuario)==false){
												passwordUsuario=null;
												usuarioEsperandoAutenticacion=null;

											}else{
												logueado=true;
											}
										}else{
											out.println(ERROR);
										}
									}else{
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

	public void commandUSER(){
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
			} else
			{
				String respuestaList = getListOfDirectory(serverDTP.getCurrentPath());
				out.println(respuestaList);
			}

		} else
		{
			out.println(ERROR);
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
			out.println(SUCCESS);
		} catch (Exception e)
		{
			out.println("ERROR");
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
			File f = new File(serverDTP.getCurrentPath()+"/"+nombreArchivo);
			if(f.exists()&&f.isFile()){
				out.println("successful_retr");
				serverDTP.sendFile(nombreArchivo);	

			}else{

				out.println("error_retr");
			}



		} catch (Exception e)
		{
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
			File f = new File(serverDTP.getCurrentPath()+"/"+nombreArchivo);
			if(f.exists()&&f.isFile()){
				System.out.println("entre al metodo dele");
				if (f.delete()) {
					out.println("successful_dele");
				} else {
					out.println("error_dele");
				}

			}else{

				out.println("error_dele");
			}



		} catch (Exception e)
		{
			out.println(ERROR);
		}

	}

	public void commandRNTO(String nuevoNombre)
	{
		try
		{	
			if(apuntador.exists()&&apuntador.isFile()&&(apuntador!=null)&&isRNFR)
			{

				File nuevoArchivo = new File(serverDTP.getCurrentPath()+"/"+nuevoNombre);
				if (apuntador.renameTo(nuevoArchivo)) {

					out.println("successful_rename");

					isRNFR = false;
				} else {

					out.println("error_rename");
					isRNFR = false;
				}
			}else{
				isRNFR = false;

				out.println("error_dele");
			}

		} catch (Exception e)
		{
			isRNFR = false;
			out.println(ERROR);
		}
	}

	public boolean commandPASS(String nombreUsuario,String passwordUsuario){
		if(nombreUsuario==null||passwordUsuario==null){
			out.println("autentication_error");

			return false;
		}else{


			try {
				//Leer el listado de Usuarios y contrase�as
				int i=0;
				BufferedReader reader = new BufferedReader(new FileReader("C:/Users/usuario/Documents/FTP/serverFTP/root/usersftp.txt"));


				String line = reader.readLine();
				boolean salir=false;
				while(line != null&&salir==false)
				{

					line = reader.readLine();
					if(line!=null){
						String [] separacion=line.split(";");

						if(separacion[0].equalsIgnoreCase(usuarioEsperandoAutenticacion)&&separacion[1].equalsIgnoreCase(passwordUsuario))
						{
							out.println("autentication_success");

							logueado=true;
							return true;

						}	
					}

				} 	
				out.println("autentication_error");
				return false;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;



		}
	}

	public void commandRNFR(String nombreArchivo)
	{
		try
		{	
			File f = new File(serverDTP.getCurrentPath()+"/"+nombreArchivo);
			if(f.exists()&&f.isFile())
			{
				isRNFR = true;
				apuntador = f;
				out.println("successful_rnfr");
			}else{
				isRNFR = false;
				out.println("error_rnfr");
				apuntador = null;
			}

		} catch (Exception e)
		{
			isRNFR = false;
			out.println(ERROR);
			apuntador = null;
		}
	}
}
