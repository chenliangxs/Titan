package rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;


public class RpcHelper {

		// Writes a JSONArray to http response.
		public static void writeJsonArray(HttpServletResponse response, JSONArray array) throws IOException {
			PrintWriter out = response.getWriter();	
			response.setContentType("application/json");
			response.addHeader("Access-Control-Allow-Origin", "*");
			out.print(array);
			out.close();
		}

	    // Writes a JSONObject to http response.
		public static void writeJsonObject(HttpServletResponse response, JSONObject obj) throws IOException {		
			PrintWriter out = response.getWriter();	
			response.setContentType("application/json");
			response.addHeader("Access-Control-Allow-Origin", "*");
			out.print(obj);
			out.close();
		}

		public static JSONObject readJSONObject(HttpServletRequest request){
			StringBuilder stringBuilder = new StringBuilder();
			try{
				BufferedReader reader = request.getReader();
				String line = null;
				while((line = reader.readLine()) != null){
					stringBuilder.append(line);
				}
				return new JSONObject(stringBuilder.toString());
			}catch(Exception e){
				//TODO: handle execption
				e.printStackTrace();
			}
			return new JSONObject();
		}
}
