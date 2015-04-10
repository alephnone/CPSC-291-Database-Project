
/* CMPUT 291 Lec B1 Mini Project 2 Phase 2
 * April 3, 2013
 * Group members: Dylan Stankievech & Alexis Tavares
 * 
 * This program implements command line arguements to sort text files according to the specification
 * run them through a perl script and then output them onto another textfile. These news textfiles are then
 * used to create database indexes using db_load to create the index idx files as specified in the spec. 
 * 
 * For some reason with larger files some of the commands simply execute when run through Java. We were unable to
 * draw a conclusion as to why this seems to happen and have opted to manually input the commands in the terminal
 * by copy and pasting them from Phase2.txt
 * 
 */



public class Phase2 {

	public static void main(String[] args) {
		
		try {
			
			/*
			 * Create string arrays cmd through cmd4 to represent the shell commands to sort uniquely (-u),
			 * numerically (-n), and pipe these outputs through the given perlscript with is then cat to the desired
			 * txt files
			 */
			String[] cmd = {
					"/bin/sh", //Shell
					"-c", //command (lets commands be read from a string)
					"sort -u prices.txt | sort -n | perl break.pl > prices_sorted.txt" //shell command 
					};
			 
			Runtime.getRuntime().exec(cmd); //runtime execution 
			
			String[] cmd2 = {
					"/bin/sh",
					"-c",
					"sort -u pdates.txt | sort -n | perl break.pl > pdates_sorted.txt"
					};
			Runtime.getRuntime().exec(cmd2);
			
			String[] cmd3 = {
					"/bin/sh",
					"-c",
					"sort -n ads.txt | perl break.pl > ads_sorted.txt"
					};
			Runtime.getRuntime().exec(cmd3);
			
			String[] cmd4 = {
					"/bin/sh",//shell
					"-c", //command
					"sort -u terms.txt | perl break.pl > terms_sorted.txt"
					};
			Runtime.getRuntime().exec(cmd4);
			
			
			
			/*
			 * Create string arrays cmd5 through cmd8 to represent the shell commands to create btree and hash indexes
			 * from the desired text files using db_load in the format:
			 * -f(read from input file) FILE -T(allow non-Berkley DB apps to load text files) -t(access method) TYPE(hash or btree) dupsort=1 OUTPUTFILE
			 */
			
			String[] cmd5 = {
					"/bin/sh", //shell
					"-c", 
					"db_load -f ads_sorted.txt -T -t hash ad.idx" //shell command
					};
			Runtime.getRuntime().exec(cmd5); //runtime execution
			
			String[] cmd6 = {
					"/bin/sh",
					"-c",
					"db_load -f terms_sorted.txt -T -t btree -c duplicates=1 te.idx"
					};
			Runtime.getRuntime().exec(cmd6);
			
			String[] cmd7 = {
					"/bin/sh",
					"-c",
					"db_load -f pdates_sorted.txt -T -t btree -c duplicates=1 da.idx"
					};
			Runtime.getRuntime().exec(cmd7);
			
			String[] cmd8 = {
					"/bin/sh",
					"-c",
					"db_load -f prices_sorted.txt -T -t btree -c duplicates=1 pr.idx"
					};
			Runtime.getRuntime().exec(cmd8);
			
			
		}
		catch (Exception e) {
			System.out.println("Exception!");
			System.err.println(e.getMessage());
		}
		
		
	}

}
