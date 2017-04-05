package com.ajourdesign.application;

import java.io.*;
import java.util.Map;
import java.net.Socket;
import java.net.ServerSocket;
import org.yaml.snakeyaml.Yaml;

public class MiniGet extends Thread {
    Socket          S;
    static String   HOST, ROOT;

    public static void main( String[] args ) throws Exception {
        String          path =(new File(".")).getAbsolutePath();
        InputStream     inpt = new FileInputStream(new File(path + File.separator + "config.yml"));
        Map             conf =(Map) (new Yaml()).load(inpt);
        int             PORT = (int)    conf.get("PORT");
                        HOST = (String) conf.get("HOST");
                        ROOT = (String) conf.get("ROOT");
        ServerSocket    sckt = new ServerSocket ( PORT );
        System.out.println("HOST: "+ HOST +"\nPORT: "+ PORT +"\nROOT: "+ ROOT );
        while( true ) new MiniGet( sckt.accept() ).start();
    }

    public MiniGet( Socket s ) throws Exception {
        S = s;
    }

    public void run() {
        String				id	= this.toString();
        try
        {	BufferedReader  I	= new BufferedReader( new InputStreamReader( S.getInputStream() ) );
            String			X	= I.readLine();
            if( X == null ) {
                I.close();
                S.close();
                Log( "*** "+ id );
                return;
            }
            PrintWriter		O	= new PrintWriter( S.getOutputStream(), true );	//	autoFlush
            int				ContentLength	= 0;
            String			UserAgent		= null,
                            Host			= null,
                            L, s;
            Log( ">>> "+ id +"\r\n\t"+ X );
            while( (L = I.readLine()) != null  &&  L.length() > 0 )
            {	s = L.toUpperCase();
                Log( "\t"+ L );
                //				  0....+....1....+.		<header>
                if( s.startsWith("CONTENT-LENGTH: "))	ContentLength = Integer.parseInt( L.substring( 16 ) );	else
                if( s.startsWith("USER-AGENT: "	   ))	UserAgent	  = L.substring( 12 );						else
                if( s.startsWith("HOST: "		   ))	Host		  = L.substring(  6 );
            }
            Log( Time_Stamp() 								+"\t"+
                    S.getRemoteSocketAddress()					+"\t"+
                    S.getInetAddress().getCanonicalHostName()	+"\t"+
                    Host										+"\t"+
                    X											+"\t["+
                    ContentLength								+"]\t"+
                    UserAgent );

            switch( "GET POSTHEADOPTI".indexOf( X.substring( 0, 4 ).toUpperCase() ) )
            {	//	 0....+....1....+....2....+....3..
                case 0:	Get ( id, X, O, S.getOutputStream(), false );   break;
            //  case 4:	Post( id, I, O, ContentLength );                      break;
                case 8: Get ( id, X, O, S.getOutputStream(), true );    break;
                case 12:
                default:Response( id, "501 Not Implemented","text/plain",0,O,"501: Not Implemented");
            }
            I.close();
            O.close();
        }
        catch( IOException e ) {
        	Log( "I/O 1: "+ e );
        }

        try{	S.close();		}
        catch(	IOException e	) {
        	Log( "I/O 2: "+ e );
        }
        System.err.println("<<< "+ id +"\r\n");
    }

    void Get( String id, String x, PrintWriter o, OutputStream O, boolean HEAD ) {
    	String[] y = x.split("\\s");	// white sp
        x = y[1];
        try {
        	int					n;
            byte				b[] = new byte[8192];
            File				src = new File( ROOT + x );
            boolean				htm = false;
            DataInputStream		dis = new DataInputStream(new BufferedInputStream(new FileInputStream( src ), 8192));
            DataOutputStream	dos = new DataOutputStream( O );

            if( src.canRead() ) {
            	switch("HTML.HTM.JS.CSS.ICO.GIF.JPG.PNG.XLS.CSV.TSV".indexOf( x.toUpperCase().substring( x.lastIndexOf(".")+1 ) ) ) {
            	//	    0....+....1....+....2....+....3....+....4
                    case 0:
                    case 5:	Response( id, "200 OK", "text/html",                src.length(), o, null );htm=true;break;
                    case 9: Response( id, "200 OK", "text/javascript",          src.length(), o, null );break;
                    case 12:Response( id, "200 OK", "text/css",                 src.length(), o, null );break;
                    case 16:
                    case 20:
                    case 24:
                    case 28:Response( id, "200 OK", "image",                    src.length(), o, null );break;
                    case 32:
                    case 46:
                    case 40:Response( id, "200 OK", "application/vnd.ms-excel", src.length(), o, null );break;
                    default:Response( id, "200 OK", "application/octet-stream", src.length(), o, null );
                }
                if( HEAD ) Log( "\tHEAD ignore" );
            	else while( (n = dis.read( b )) > 0 )
                    dos.write( (htm ? (new String( b )).replaceFirst( "HOST", HOST ).getBytes() : b), 0, n );
            }
            else Response( id, "403 Forbidden", "text/html", 0, o, Wrn("<blink>Forbidden: </blink>"+ x ) );
            dos.flush();
            Thread.sleep(256);
            dos.close();
            dis.close();
        }
        catch( IOException e ) {
        	Log("I/O 3: "+ e );
            Response( id, "404 Not Found", "text/html", 0, o, Wrn("<blink>Not Found</blink>") );
        }
        catch( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    void Response( String id, String code, String type, long length, PrintWriter o, String text ) {
    	Log("... "+ id +"\r\n"+
            "\tHTTP/1.1 "+ code +"\r\n\tDate: "+ (new java.util.Date()) +
            "\r\n\tServer: J3W 2017-04-10\r\n\tContent-Length: "+ (text == null ? length : text.length()) +
            "\r\n\tContent-Type: "+ type );
        o.print(
            "HTTP/1.1 "+ code +"\r\nDate: "+ (new java.util.Date()) +
            "\r\nServer: J3W 2017-04-10\r\nContent-Length: "+ (text == null ? length : text.length()) +
            "\r\nContent-Type: "+ type +"\r\n\r\n" );
        if( text != null ) o.print( text );
        o.flush();
    }

    String Wrn( String s ) {
    	return "<html><title>J3W</title><body><h1><br>"+ s +"</h1></body></html>";
    }

    String Txt( String s ) {
    	return "<html><title>J3W</title><body><br><pre>\r\n"+ s +"</pre></body></html>";
    }

    static void	Log( String s ) {
    	System.err.println( s );
    //  System.out.println( s );
    }

    String Time_Stamp() {
    	java.util.Date	x = new java.util.Date();
        int				Y = x.getYear(),
                        M = x.getMonth()+1,
                        D = x.getDate(),
                        h = x.getHours(),
                        m = x.getMinutes(),
                        s = x.getSeconds(),
                        c = (int) Math.floor( (x.getTime() % 1000) * 0.1 );
        return(	      Integer.toString(Y  < 1900 ? Y+1900 : Y )			+"-"+
            ( M > 9 ? Integer.toString(M) : ("0"+Integer.toString(M)) )	+"-"+
            ( D > 9 ? Integer.toString(D) : ("0"+Integer.toString(D)) )	+"\t"+
            ( h > 9 ? Integer.toString(h) : ("0"+Integer.toString(h)) )	+"."+
            ( m > 9 ? Integer.toString(m) : ("0"+Integer.toString(m)) )	+"."+
            ( s > 9 ? Integer.toString(s) : ("0"+Integer.toString(s)) )	+"."+
            ( c > 9 ? Integer.toString(c) : ("0"+Integer.toString(c)) ) );
    }
}