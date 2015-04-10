/* CMPUT 291 Lec B1 Mini Project 2 Phase 3
 * April 3rd, 2013
 * Group members: Dylan Stankievech & Alexis Tavares
 * 
 * 
 * This program processses the following queries givent the index files Given the index files ad.idx, te.idx, da.idx and pr.idx 
 * created in Phase 2 respectively on ad ids, terms, dates, and prices:
 * 
 * Each query returns the full record of the matching ads, with ad id first, followed by the rest of the fields formatted for output display, 
 * which should are readable and not in xml. Each query defines some conditions that must be satisfied by title, body, post data and the price 
 * fields of the matching records:
 * 
 * A condition can be either an exact match or a partial match; partial matches are restricted to prefix matches only 
 * 
 * (i.e. the wild card % can only appear at the end of a term). 
 * 
 * All matches are case-insensitive, hence the queries "Camera", "camera", "cAmera" would retrieve
 * the same results; for the same reason the extracted terms in previous phases are all stored in lowercase. 
 * 
 * Matches on dates and prices are range conditions denoted by since and until for dates and price < and price > for price.
 * 
 * There is one or more spaces between the keywords since and until and the date that follows it. There is zero or more spaces between price, 
 * the symbols < and > and the numbers that follow.
 * 
 *  Matches on terms can be exact or partial
 * 
 * A query can have multiple conditions in which case the result must match all those conditions (i.e. the and semantics), and there is one 
 * or more spaces between the conditions.
 * 
 *  The keywords price, since and until are reserved for searches on dates and prices (as described above) and would 
 * not be used for any other purposes. 
 * 
 * The dates are formatted as yyyy/mm/dd in both queries and in the data.
 * 
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.sleepycat.db.Cursor;
import com.sleepycat.db.Database;
import com.sleepycat.db.DatabaseConfig;
import com.sleepycat.db.DatabaseEntry;
import com.sleepycat.db.DatabaseType;
import com.sleepycat.db.LockMode;
import com.sleepycat.db.OperationStatus;


public class Phase3 {

	static Database std_db;
	static String query; //the users query
	static boolean firstQuery = true; 
	static List<String> adIds = new ArrayList<String>(); //array list for displaying ads
	
	public static void show_usage(){
		//shows the user what format their queries must follow
		System.out.println("Queries must follow the following rules:");
		System.out.println("(1)A condition can be either an exact match or a partial match; for simplicity, partial matches are restricted to prefix matches only (i.e. the wild card % can only appear at the end of a term).");
		System.out.println("(2)All matches are case-insensitive, hence the queries Camera, camera, cAmera would retrieve the same results");
		System.out.println("(3)Matches on dates and prices are range conditions denoted by since and until for dates and price < and price > for price");
		System.out.println("(4)There is one or more spaces between the keywords since and until and the date that follows it. There is zero or more spaces between price, the symbols < and > and the numbers that follow, hence, price<20, price< 20, price <20, and price     <    20 are all valid and would return the same matches");
		System.out.println("(5) Matches on terms can be exact or partial. query can have multiple conditions, and there is one or more spaces between the conditions");
		System.out.println("(6)The keywords price, since and until are reserved for searches on dates and prices (as described above) and would not be used for any other purposes. The dates are formatted as yyyy/mm/dd in queries");
		
	}

	public static void main(String[] args) {
		
		Scanner scan = new Scanner(System.in);
		
		
		
		//Loop allowing multiple queries?
		show_usage;
		System.out.println("Enter your query:");
		
		query = scan.nextLine();
		query = compact_whitespace(query);
		query = query.toLowerCase();
		
		//Check the query for all possible input conditions over and over until there are none
		while(!query.isEmpty()) {
		
			if( (query.charAt(0) =='t' || query.charAt(0) == 'b') && query.charAt(1) == '-')
			{
				//for body or title keywords, can possibly have a '%' at end
				term_keyword();
			}
	
			
			else if( query.startsWith("since "))
			{
				//must be exactly one space between 'since' and the date, since whitespace was compacted
				since_date();
			}
			
			else if( query.startsWith("until ") )
			{
				//must be exactly one space between 'until' and the date, since whitespace was compacted
				until_date();
			}
			
			else if( query.startsWith("price >") || query.startsWith("price>"))
			{
				//either 1 or 0 space between 'price' and '>'
				price_greater_than();
			}
			
			else if (query.startsWith("price <") || query.startsWith("price<"))
			{
				//either 1 or 0 spaces between 'price' and '<'
				price_less_than();
			}
			
			else 
			{
				//it's a keyword, search in both body and title, can this have '%' at end?
				keyword_search();
			}
		
		}
		
		System.out.println("Number of ads: " + adIds.size());
		if(adIds.isEmpty()) {
			System.out.println("No ads match query!");
		}
		else {
			for (String finalId : adIds) {
				print_ad(finalId);
			}
		}
		
	}
	
	/*
	 * Search by term_keyword by creating a new database on our indexes and finding all
	 * instances of the keyword substring within the users query in that database. Checks that
	 * the keyword from the given key of the current database element is the same as our 
	 * keyword. If true, adds that element to a temporary list which is then added to the global
	 * ad_list variable. If false, the search ends and with all previous results added to ad_list.
	 * 
	 * Handles wildcard terms by removing the wildcard delimeter and then searching for all 
	 * partial db matches
	 * */
	static void term_keyword(){

		DatabaseConfig dbConfig = new DatabaseConfig();

		dbConfig.setType(DatabaseType.BTREE);
		
		dbConfig.setUnsortedDuplicates(true);
		

		try {
			//new database using our terms btree index
			std_db = new Database("te.idx", null, dbConfig);
			
			
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			//obtain keyword from users query
			String keyword = get_word(query);
			query = remove_word(query);
			
			//Deals with wildcard (%) for partial matching in the database
			if (keyword.endsWith("%")) {
				keyword = keyword.substring(0,keyword.length() - 1);
				key.setData(keyword.getBytes());
				key.setSize(keyword.length());
				key.setPartial(true);
				
				Cursor cursor = std_db.openCursor(null,  null);
				OperationStatus oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
				List<String> tempIds = new ArrayList<String>(); //temporary structure to hold our result set
				while(oprstatus == OperationStatus.SUCCESS) {
					String keyword2 = new String(key.getData()); //get keyword 

					if (!keyword2.startsWith(keyword)) {
						break; //if we encounter a result that does not start with our keyword stop looping and print out the current result set
					}
					//add current result to our temp structure, then move on to the next item in the database
					String id = new String(data.getData());
					tempIds.add(id);
					data = new DatabaseEntry();
					key = new DatabaseEntry();
					oprstatus = cursor.getNext(key, data, LockMode.DEFAULT);
					
					
				}
				if (firstQuery == true) {
					firstQuery = false;
					adIds.addAll(tempIds); //create finalized list off all results
				}
				else {
					list_intersection(tempIds); //intersect current result set with the queried results
				}
			}
			else {

				//no wildcards used search for the keyword
			
				key.setData(keyword.getBytes());
				key.setSize(keyword.length());
				
				Cursor cursor = std_db.openCursor(null,  null);
				OperationStatus oprstatus = cursor.getSearchKey(key,  data, LockMode.DEFAULT);
				List<String> tempIds = new ArrayList<String>();
				while(oprstatus == OperationStatus.SUCCESS) {
					String id = new String(data.getData());
					tempIds.add(id);
					data = new DatabaseEntry();
					oprstatus = cursor.getNextDup(key, data, LockMode.DEFAULT);
					
				}
				if (firstQuery == true) {
					firstQuery = false;
					adIds.addAll(tempIds);
				}
				else {
					list_intersection(tempIds);
				}
			}
			//closes the current database
			std_db.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	/*Deals with the 'since' delimeter by removing the 'since' from the keyword, getting and
	 * removing the date and from the keyword and then adding all matching db elements to a temp
	 * list which is then added to the ad_list global variable to store all of the matching elments 
	 * from the query
	 * */
	static void since_date(){
		DatabaseConfig dbConfig = new DatabaseConfig();

		dbConfig.setType(DatabaseType.BTREE);
		
		dbConfig.setUnsortedDuplicates(true);
		

		try {
			std_db = new Database("da.idx", null, dbConfig);
			
			
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			query = remove_word(query); //Removing 'since '
			String keyword = get_word(query); //Getting the date
			query = remove_word(query); //Removing the date
			
			key.setData(keyword.getBytes());
			key.setSize(keyword.length());
			
			Cursor cursor = std_db.openCursor(null,  null);
			OperationStatus oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
			List<String> tempIds = new ArrayList<String>();
			while(oprstatus == OperationStatus.SUCCESS) {
				String id = new String(data.getData());
				tempIds.add(id);
				data = new DatabaseEntry();
				oprstatus = cursor.getNext(key, data, LockMode.DEFAULT); //progress the cursor
				
			}
			if (firstQuery == true) {
				firstQuery = false;
				adIds.addAll(tempIds);
			}
			else {
				list_intersection(tempIds);
			}
			std_db.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	/*Deals with the 'until' delimeter by removing the 'until' from the keyword, getting and
	 * removing the date and from the keyword and then adding all matching db elements to a temp
	 * list which is then added to the ad_list global variable to store all of the matching elments 
	 * from the query
	 * */
	static void until_date(){
		DatabaseConfig dbConfig = new DatabaseConfig();

		dbConfig.setType(DatabaseType.BTREE);
		
		dbConfig.setUnsortedDuplicates(true);
		

		try {
			std_db = new Database("da.idx", null, dbConfig);
			
			
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			query = remove_word(query); //Remove 'until '
			String keyword = get_word(query); //Get the date
			query = remove_word(query); //Remove the date
			
			key.setData(keyword.getBytes());
			key.setSize(keyword.length());
			
			Cursor cursor = std_db.openCursor(null,  null);
			OperationStatus oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
			List<String> tempIds = new ArrayList<String>();
			while(oprstatus == OperationStatus.SUCCESS) {
				String id = new String(data.getData());
				tempIds.add(id);
				data = new DatabaseEntry();
				oprstatus = cursor.getPrev(key, data, LockMode.DEFAULT);
				
			}
			if (firstQuery == true) {
				firstQuery = false;
				adIds.addAll(tempIds);
			}
			else {
				list_intersection(tempIds);
			}
			std_db.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		
		
	}
	/*Handles the '<'delimeter by getting the price substring from the current query, 
	 * padding it with whitespace and then gathering all db elements in a new database bashed off of the 
	 * betree index on prices  smaller than the price in our query.
	 * */
	static void price_less_than(){
		DatabaseConfig dbConfig = new DatabaseConfig();

		dbConfig.setType(DatabaseType.BTREE);
		
		dbConfig.setUnsortedDuplicates(true);
		

		try {
			std_db = new Database("pr.idx", null, dbConfig);
			
			
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			
			if (query.startsWith("price < "))
			{
				query = query.substring(8);
			}
			else if (query.startsWith("price <"))
			{
				query = query.substring(7);
			}
			else if (query.startsWith("price< "))
			{
				query = query.substring(7);
			}
			else if (query.startsWith("price<"))
			{
				query = query.substring(6);
			}
			else {
				System.out.print("THIS SHOULDN'T HAPPEN!\n");
				System.exit(0);
			}
			
			//pad with white space
			String price = get_word(query);
			String padded_price = price;
			int num_blanks = 8 - padded_price.length();
			while (num_blanks > 0) {
				padded_price = ' ' + padded_price;
				num_blanks--;
			}
			query = remove_word(query);
			
			key.setData(padded_price.getBytes());
			key.setSize(padded_price.length());
			
			Cursor cursor = std_db.openCursor(null,  null);
			OperationStatus oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
			String compare_price = new String(key.getData());
			compare_price = remove_whitespace(compare_price);
			
			//get all matching prices less than the price given in our query and ad these prices to adIds global var
			while (Integer.parseInt(compare_price) >= Integer.parseInt(price) && oprstatus == OperationStatus.SUCCESS) {
				oprstatus = cursor.getPrev(key, data, LockMode.DEFAULT);
				compare_price = new String(key.getData());
				compare_price = remove_whitespace(compare_price);
			}
			List<String> tempIds = new ArrayList<String>();
			while(oprstatus == OperationStatus.SUCCESS) {
				String id = new String(data.getData());
				tempIds.add(id);
				data = new DatabaseEntry();
				oprstatus = cursor.getPrev(key, data, LockMode.DEFAULT);
				
			}
			if (firstQuery == true) {
				firstQuery = false;
				adIds.addAll(tempIds);
			}
			else {
				list_intersection(tempIds);
			}
			std_db.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		
		
		
	}
	
	/*Handles the '>'delimeter by getting the price substring from the current query, 
	 * padding it with whitespace and then gathering all db elements in a new database bashed off of the 
	 * betree index on prices larger than the price in our query.
	 * */
	static void price_greater_than(){
		DatabaseConfig dbConfig = new DatabaseConfig();

		dbConfig.setType(DatabaseType.BTREE);
		dbConfig.setUnsortedDuplicates(true);
		

		try {
			//Create new database using our btree index
			std_db = new Database("pr.idx", null, dbConfig);
			
			
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			//allows multiple different spaces between price and ">"
			//append the query to just the numerical price on the LHS of ">"
			if (query.startsWith("price > "))
			{
				query = query.substring(8);
			}
			else if (query.startsWith("price >"))
			{
				query = query.substring(7);
			}
			else if (query.startsWith("price> "))
			{
				query = query.substring(7);
			}
			else if (query.startsWith("price>"))
			{
				query = query.substring(6);
			}
			else {
				System.out.print("THIS SHOULDN'T HAPPEN!\n");
				System.exit(0);
			}
			
			String price = get_word(query);
			String padded_price = price;
			
			//pad the prices with whitespace
			int num_blanks = 8 - padded_price.length();
			while (num_blanks > 0) {
				padded_price = ' ' + padded_price;
				num_blanks--;
			}
			
			
			query = remove_word(query);
			
			key.setData(padded_price.getBytes());
			key.setSize(padded_price.length());
			
			Cursor cursor = std_db.openCursor(null,  null);
			OperationStatus oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
			String compare_price = new String(key.getData());
			compare_price = remove_whitespace(compare_price);
			while (Integer.parseInt(compare_price) <= Integer.parseInt(price) && oprstatus == OperationStatus.SUCCESS) {
				oprstatus = cursor.getNext(key, data, LockMode.DEFAULT);
				compare_price = new String(key.getData());
				compare_price = remove_whitespace(compare_price);
			}
			List<String> tempIds = new ArrayList<String>();
			while(oprstatus == OperationStatus.SUCCESS) {
				String id = new String(data.getData());
				tempIds.add(id);
				data = new DatabaseEntry();
				oprstatus = cursor.getNext(key, data, LockMode.DEFAULT);
				
			}
			//if firstQuery is true, set it to false and place all of our returned ads 
			//from the search into adIds. Otherwise, get the intersection of adIds and the list of
			//returned ads
			if (firstQuery == true) {
				firstQuery = false;
				adIds.addAll(tempIds);
			}
			else {
				list_intersection(tempIds);
			}
			std_db.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		
		
		
	}
	
	/*
	 * Search the title or body of an ad using a keyword taken from the users query. 
	 * 
	 * */
	static void keyword_search(){
	
		//Search both title and body, a match in either is acceptable
		
		DatabaseConfig dbConfig = new DatabaseConfig();

		dbConfig.setType(DatabaseType.BTREE);
		dbConfig.setUnsortedDuplicates(true);
		

		try {
			//create new database using our btree index
			std_db = new Database("te.idx", null, dbConfig);
			
			
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			//search for t-KEYWORD as this is how members of our data appear
			String keyword = get_word(query);
			query = remove_word(query);
			
			
			//% for partial matching
			if (keyword.endsWith("%")) {
				keyword = keyword.substring(0,keyword.length() - 1);
				
				String title_keyword = "t-" + keyword;
				key.setData(title_keyword.getBytes());
				key.setSize(title_keyword.length());
				key.setPartial(true);
				
				Cursor cursor = std_db.openCursor(null,  null);
				OperationStatus oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
				List<String> tempIds1 = new ArrayList<String>();
				while(oprstatus == OperationStatus.SUCCESS) {
					String keyword2 = new String(key.getData());

					if (!keyword2.startsWith(title_keyword)) {
						break;
					}
					String id = new String(data.getData());
					tempIds1.add(id);
					data = new DatabaseEntry();
					key = new DatabaseEntry();
					oprstatus = cursor.getNext(key, data, LockMode.DEFAULT);
					
					
				}
				
				String body_keyword = "b-" + keyword;
				key.setData(body_keyword.getBytes());
				key.setSize(body_keyword.length());
				key.setPartial(true);
				
				oprstatus = cursor.getSearchKeyRange(key,  data, LockMode.DEFAULT);
				List<String> tempIds2 = new ArrayList<String>();
				while(oprstatus == OperationStatus.SUCCESS) {
					String keyword2 = new String(key.getData());
					
					if (!keyword2.startsWith(body_keyword)) {
						break;
					}
					String id = new String(data.getData());
					tempIds2.add(id);
					data = new DatabaseEntry();
					key = new DatabaseEntry();
					oprstatus = cursor.getNext(key, data, LockMode.DEFAULT);
					
				}
				
				for (String element : tempIds2) {
					if (!tempIds1.contains(element)) {
						tempIds1.add(element);
					}
				}
				
				if (firstQuery == true) {
					firstQuery = false;
					adIds.addAll(tempIds1);
				}
				else {
					list_intersection(tempIds1);
				}
			}
			
			//No %
			else {
				String term_keyword = "t-" + keyword;
				
				
				key.setData(term_keyword.getBytes());
				key.setSize(term_keyword.length());
				
				Cursor cursor = std_db.openCursor(null,  null);
				OperationStatus oprstatus = cursor.getSearchKey(key,  data, LockMode.DEFAULT);
				
				//create an array list of the results and fill it with all matching results
				List<String> tempIds1 = new ArrayList<String>();
				while(oprstatus == OperationStatus.SUCCESS) {
					String id = new String(data.getData());
					tempIds1.add(id);
					data = new DatabaseEntry();
					oprstatus = cursor.getNextDup(key, data, LockMode.DEFAULT);
					
				}
				
				//search for b-KEYWORD as this is how members of our data appear
				String body_keyword = "b-" + keyword;
				key.setData(body_keyword.getBytes());
				key.setSize(body_keyword.length());
				oprstatus = cursor.getSearchKey(key,  data, LockMode.DEFAULT);
				
				//create array list of the results and fill it with all matching results
				List<String> tempIds2 = new ArrayList<String>();
				while(oprstatus == OperationStatus.SUCCESS) {
					String id = new String(data.getData());
					tempIds2.add(id);
					data = new DatabaseEntry();
					oprstatus = cursor.getNextDup(key, data, LockMode.DEFAULT);
					
				}
				
				//for all elemets of tempIds2, if they don't already exist in tempIds1 add them
				for (String element : tempIds2) {
					if (!tempIds1.contains(element)) {
						tempIds1.add(element);
					}
				}
				
				//if firstQuery is true, set it to false and place all of our returned ads 
				//from the search into adIds. Otherwise, get the intersection of adIds and the list of
				//returned ads
				if (firstQuery == true) {
					firstQuery = false;
					adIds.addAll(tempIds1);
				}
				else {
					list_intersection(tempIds1);
				}
			}
			std_db.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		
	}
	
	/*
	 *Prints the given ad in the format:
	 * 
	 * */
	//Need to change output format
	static void print_ad(String id) {
		
		DatabaseConfig dbConfig = new DatabaseConfig();
		//set what type of index
		dbConfig.setType(DatabaseType.HASH);
		
		
		try {
			//new database using our Hash index
			std_db = new Database("ad.idx", null, dbConfig);
			
			//inserting entries
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			
			key.setData(id.getBytes());
			key.setSize(id.length());
			
			Cursor cursor = std_db.openCursor(null,  null);
			OperationStatus oprstatus = cursor.getSearchKey(key,  data, LockMode.DEFAULT);

				String xml = new String(data.getData());
				System.out.println(xml);
				//print_full_ad(id);
				oprstatus = cursor.getNextDup(key, data, LockMode.DEFAULT);
			std_db.close();

		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	//Gets the intersection of the elements in adIds and this temp List
	//And update adIds to only have these elements
	static void list_intersection(List<String> temp) {
		List<String> temp2 = new ArrayList<String>();

		for ( String element : adIds) {
			if (temp.contains(element)) {
				temp2.add(element);	
			}
		}
		adIds = temp2;
	}
	
	//Convert any multiple whitespace in a string sinto a single space
	static String compact_whitespace(String input) {
		String compacted = input.replaceAll("\\s+", " ");
		return compacted;
	}
	
	//Remove all whitespace from a string
	static String remove_whitespace(String input) {
		String removed = input.replaceAll("\\s+", "");
		return removed;
	}
	
	//Get the first word of a string, up until the first space encountered
	//If no spaces are encountered, then returns the whole string
	static String get_word(String input) {
		int i = input.indexOf(' ');
		String first_word = input;
		if (i != -1) {
			first_word = input.substring(0, i);
		}
		return first_word;
	}
	
	//Removes the first word of a string, which is up until the first space
	//If no spaces are encountered, then returns an empty string
	static String remove_word(String input) {
		int i = input.indexOf(' ');
		String removed = "";
		if (i != -1) {
			removed = input.substring(i+1);
		}
		return removed;
	}
}
