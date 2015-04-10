
/* CMPUT 291 Lec B1 Mini Project 2 Phase 1
 * April 3rd, 2013
 * Group members: Dylan Stankievech & Alexis Tavares
 * 
 * 
 * This program is users a Java library parser to parse an XML file and produces 4 files as follows:
 * 
 * terms.text with includes term extracted from the title and bodies of ads following the format
 * 	            t-(TERM IN TITLE LOWERCASE):(AD ID) for titles and b-(TERM IN BODY LOWERCASE):(AD ID)
 * 				for bodies
 * 
 * pdates.txt with one line for each ad in the form of (POST DATE):(AD ID)
 *  
 * prices.txt with one line for each ad that has a non-empty price field in the form of (PRICE):(AD ID)
 * 
 * ads.txt with one line for each ad in the form of (AD ID):(AD RECORD IN XML)
 * 
 * 
 */




import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.PrintWriter;


public class Phase1 {

	
	
	public static void main(String argv[]) {
		
		/* Parse a given XML file using DOM library parser which creates a NodeList of required ad elements. 
		 * The node list is then accessed element by element with the current element being subject to and 
		 * tested for the specifications of the output files
		 * 
		 */
		 
		 Scanner scan  = new Scanner(System.in);
		 System.out.println("Please enter the name (without .xml) of the file you wish to parse. If the file is not in the home direction please include the path");
		 String file = scan.nextLine();
		 
	    try {
		
			//Create a new document from XML file and parse using DOM
			File fXmlFile = new File(file+".xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
		
			//Normalize our xml document
			doc.getDocumentElement().normalize();
		 
			//Create new node list of Ad elements from the parsed document
			NodeList nList = doc.getElementsByTagName("ad");
		
			//Init PrintWriter obj which writes our parsed xml into seperate txt files
			PrintWriter termsOut = new PrintWriter("terms.txt");
			PrintWriter pdateOut = new PrintWriter("pdates.txt");
			PrintWriter priceOut = new PrintWriter("prices.txt");
			PrintWriter adOut = new PrintWriter("ads.txt");
			
			//Go through the entire  NodeList element by element
			for (int temp = 0; temp < nList.getLength(); temp++) {
		 
				Node nNode = nList.item(temp);
				
		 
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					
					Element eElement = (Element) nNode;
					

					//Create string elements of the text content of the elements we need to use from our XML doc
					String id = eElement.getElementsByTagName("id").item(0).getTextContent();
					String title = eElement.getElementsByTagName("title").item(0).getTextContent();
					String body = eElement.getElementsByTagName("body").item(0).getTextContent();
					String pdate = eElement.getElementsByTagName("pdate").item(0).getTextContent();
					String price = eElement.getElementsByTagName("price").item(0).getTextContent();
					
					//Create string with xml formatting
					String full = "<ad><id>" + id + "</id><title>" + title + "</title><body>" + body + "</body><price>" + price + "</price><pdate>" + pdate + "</pdate></ad>";
					
					//replace reserved characters for correct xml formatting
					full = full.replaceAll("&", "&amp;");
					full = full.replaceAll("\"", "&quot;");
					full = full.replaceAll("'", "&apos;");
				
					//output this xml format onto the ads.text as given in spec
					adOut.println(id + ":" + full);
					
					//print pdate to pdates.txt
					pdateOut.println(pdate + ":" + id);
					
					//If price is not empty, pad  with white spaces so db_load loads them correctly and then print to prices.txt
					if (!price.isEmpty()){
						int num_blanks = 8 - price.length();
						while(num_blanks > 0) {
							price = ' ' + price;
							num_blanks--;
						}
						
						priceOut.println(price + ":" + id);
					}
					
					//replace all alphanumeric characters with whitespace, split on whitespace and print to terms.text
					title = title.replaceAll("[^A-Za-z0-9_]", " ");
	
					String[] titleTerms = title.split("\\s+");
					
					for (int i =0; i < titleTerms.length; i++) {
						String term = titleTerms[i];
						if (term.length() > 2) {
							termsOut.println("t-" + term.toLowerCase() + ":" + id);
						}
						
					}
					
					//replace all alphanumeric characters with whitespace, split on whitespace and print to terms.text
					body = body.replaceAll("[^A-Za-z0-9_]", " ");
	
					String[] bodyTerms = body.split("\\s+");
					
					for (int i =0; i < bodyTerms.length; i++) {
						String term = bodyTerms[i];
						if (term.length() > 2) {
							termsOut.println("b-" + term.toLowerCase() + ":" + id);
						}
						
					}
				}
			}
			//Close all PrintWriter objects
			adOut.close();
			priceOut.close();
			pdateOut.close();
			termsOut.close();
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
	  }
}
