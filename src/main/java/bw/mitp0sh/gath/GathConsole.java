package bw.mitp0sh.gath;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GathConsole {
	
	/* default root */
	private static String root = "https://opensource.apple.com";

	/* default is every 60 minutes */
	private static long interval = 10;
	
	/* default product to check */
	private static String[] products = {
		"https://opensource.apple.com/release/macos-10152.html",
	};
	
	private static boolean ignoreComponents = true;
	
	/* default whitelist to apply */
	private static final ArrayList<HashMap<String, String[]>> WHITELIST = new ArrayList<>();
	
	public static void main(String[] args) {
		
		System.out.println("GATH, Golden Apple Tree Harvester v0.1 by mitp0sh[PDX]");
		System.out.println("Copyright 2020, All rights unreserved.");
		System.out.println("https://twitch.tv/mitp0sh_of_pdx | mitp0sh@mitp0sh.de\n");
		
		parseConfig();   
		parseMailConfig();
		
		while(true) {
			File repository = new File("repository");
			if(repository.isFile()) {
				repository.delete();
				repository.mkdir();
			} else if(!repository.exists()) {
				repository.mkdir();
			}
			
			String[] productDirStrings = new String[products.length];			
			int i = 0;
			for(String product : products) {
				String productStr = (product.split("\\/")[product.split("\\/").length-1]).split("\\.")[0];
				productDirStrings[i] = productStr;
				i++;
			}
			
			File[] productDirFiles = new File[products.length];
			for(i = 0; i < products.length; i++) {
				
				System.out.println("[*] processing - " + products[i] + "\"");
				
				productDirFiles[i] = new File(repository, productDirStrings[i]);
				if(productDirFiles[i].isFile()) {
					productDirFiles[i].delete();
					productDirFiles[i].mkdir();
				} else if(!productDirFiles[i].exists()) {					
					productDirFiles[i].mkdir();
				}
				
				File currentIndexFile = new File(productDirFiles[i], productDirStrings[i] + ".html");
				if(currentIndexFile.exists() && currentIndexFile.isFile()) {
					currentIndexFile.delete();
				}
				
				try {
					currentIndexFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try (BufferedInputStream in = new BufferedInputStream(new URL(products[i]).openStream());
				  FileOutputStream fileOutputStream = new FileOutputStream(currentIndexFile)) {
					byte dataBuffer[] = new byte[1024];
				    int bytesRead;
				    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				        fileOutputStream.write(dataBuffer, 0, bytesRead);
				    }
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Instant.now().getEpochSecond() - UNIX TIME STAMP!!!
				String hash = calculateMD5(currentIndexFile);		    		    
			    File fileCopy = new File(currentIndexFile.getParentFile(), productDirStrings[i] + "-" + hash + ".html");
			    if(!fileCopy.exists()) {
			    	Path originalPath = currentIndexFile.toPath();
			    	Path copy = fileCopy.toPath();
				    try {
						Files.copy(originalPath, copy, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
			    }	
			    
			    File hashDir = new File(currentIndexFile.getParentFile(), hash);
			    if(!hashDir.exists()) {
			    	hashDir.mkdir();
			    }
			    
			    ArrayList<String> listing = new ArrayList<>();
			    ArrayList<String> listingGroup = new ArrayList<>();
			    ArrayList<String> listingTar = new ArrayList<>();
			    ArrayList<String> listingRelPath = new ArrayList<>();
			    
			    FileReader fr;
				try {
					fr = new FileReader(currentIndexFile);
					BufferedReader br = new BufferedReader(fr);
				    String line;  
				    boolean needNext = false;			     
				    while((line = br.readLine()) != null) {  
				    	line = line.strip();			    	
				    	if(needNext && line.length() != 0) {
				    		if(line.equals("-")) { // special case if release is still missing */
				    			needNext = false;
			    				continue;
				    		}
				    		
				    		String group = line.split("\\/")[2];			    		
				    		String tar = line.split("\\/")[3].split("\\\"")[0];
				    		String relPath = line.split("\\\"")[1];
				    		
				    		boolean found = false;
				    		if(!ignoreComponents) {
				    			Iterator<HashMap<String, String[]>> whiteListIter = WHITELIST.iterator();
					    		while(whiteListIter.hasNext()) {
					    			HashMap<String, String[]> map = whiteListIter.next();
					    			String key = map.keySet().iterator().next();
					    			if(!productDirStrings[i].equals(key)) {				    				
					    				continue;
					    			}
					    			
					    			String[] compElems = map.get(key);
					    			for(String elem : compElems) {
					    				if(elem.equals(group)) {
					    					found = true;
					    					break;
					    				}
					    			}
					    			
					    			if(!found) {				    				
					    				continue;
					    			} else {
					    				break;
					    			}
					    		}
				    		}
				    		
				    		if(!found) {
			    				needNext = false;
			    				continue;
			    			} 
				    		
				    		listingRelPath.add(relPath);
				    		listingTar.add(tar);
				    		listingGroup.add(group);
				    		listing.add(line);
				    		needNext = false;
				    	} else 
				    	if(line.indexOf("<td class=\"project-downloads\">") != -1) {
				    		needNext = true;
				    	}   
				    }  
				    fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}			
				
				for(int y = 0; y < listingRelPath.size(); y++) {
					try {
						File outFile = new File(hashDir, listingTar.get(y) + ".tmp");
						if(outFile.exists() && outFile.isFile()) {
							outFile.delete();
							outFile.createNewFile();
						}
						
						ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(root + listingRelPath.get(y)).openStream());
						FileOutputStream fileOutputStream = new FileOutputStream(new File(hashDir, listingTar.get(y) + ".tmp"));					
						FileChannel channel = fileOutputStream.getChannel();					
						channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
						channel.close();
						fileOutputStream.close();
						readableByteChannel.close();
						
						String outFileHash = calculateMD5(outFile);				
						File hashedOutFile = new File(hashDir, listingTar.get(y) + "_" + outFileHash + ".tar.gz");					
						if(!hashedOutFile.exists()) {
							Files.move(outFile.toPath(), hashedOutFile.toPath());
							System.out.println("[+] modification detected, file = " + listingRelPath.get(y) + ", md5 = " + outFileHash + ", timestamp = " + Instant.now().getEpochSecond());
						} else {							
							outFile.delete();
						}					
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			try {
				Thread.sleep(interval*1000*60); // minute resolution
			} catch (InterruptedException e) {}
		}
	}
	
	private static String calculateMD5(File file) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(Files.readAllBytes(Paths.get(file.toURI())));
		    byte[] digest = md.digest();
		    return new BigInteger(1,digest).toString(16);		    
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
	    
		return null;
	}
	
	private static void parseConfig() {
		JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader("config.json")) {
        	JSONObject jsonConfig = (JSONObject) parser.parse(reader);
        	
        	/* check root value*/
        	try {
        		String root = (String) jsonConfig.get("root");    
        		new URL(root).toURI();        		
        		/* apply root value */
        		GathConsole.root = root;
        	} catch(NullPointerException | ClassCastException | URISyntaxException e) {/* keep default value */}
        	
        	/* check interval */
        	try {
        		long interval = (long) jsonConfig.get("interval");
        		if(interval > 5) {
        			/* apply interval*/
        			GathConsole.interval = interval;
        		}
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	/* check products */
        	try {
        		JSONArray jsonProducts = (JSONArray) jsonConfig.get("products");
            	int numProducts = jsonProducts.size();
        		if(numProducts != 0) {
        			String[] products = new String[jsonProducts.size()];
        			@SuppressWarnings("unchecked")
					Iterator<String> iterator = jsonProducts.iterator();
                    int p = -1;
        			while (iterator.hasNext()) {
        				p++;
        				String url = iterator.next(); 
        				new URL(url).toURI();
        				products[p] = url;
                    }        			
        			/* apply products */
        			GathConsole.products = products;
            	}        		
        	} catch(NullPointerException | ClassCastException | URISyntaxException e) {/* keep default value */}
        	
        	/* check ignore-interval */
        	try {
        		boolean ignoreComponents = (Boolean) jsonConfig.get("ignore-components");
        		GathConsole.ignoreComponents = ignoreComponents;
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	try {
        		JSONArray jsonComponents = (JSONArray)jsonConfig.get("components");
        		@SuppressWarnings("unchecked")
        		Iterator<JSONObject> iterator = jsonComponents.iterator();                
    			while (iterator.hasNext()) {    				
    				JSONObject component = iterator.next();
    				String key = (String)component.keySet().iterator().next();
    				HashMap<String, String[]> compMap = new HashMap<>();    				
    				JSONArray componentElements = (JSONArray)component.get(key);
    				String[] compElements = new String[componentElements.size()];    				
    				@SuppressWarnings("unchecked")
					Iterator<String> compIter = componentElements.iterator();
    				int p = -1;
    				while(compIter.hasNext()) {
    					p++;
    					compElements[p] = compIter.next();
    				}
    				compMap.put(key, compElements);
    				WHITELIST.add(compMap);    				
                }    
        	} catch(NullPointerException | ClassCastException e) { e.printStackTrace();/* keep default value */}
        	
        	System.out.println("[*] config - interval = " + GathConsole.interval + ", ignore-components=" + ignoreComponents);
        } catch (FileNotFoundException e1) {
        	System.err.println("[-] error - config.json missing ;(");
        	return;
		} catch (IOException e1) {
			System.err.println("[-] error - bruh, your IO is screwed ;(");
			return;
		} catch (ParseException e) {
			System.err.println("[-] error - it's all about structure, dude! Watch your config.json");
			return;
		}
	}
	
	private static void parseMailConfig() {
		JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader("mail.json")) {
        	JSONObject jsonMailConfig = (JSONObject) parser.parse(reader);
        	
        	String username = null;
        	try {
        		username = (String) jsonMailConfig.get("username");
        		if(username == null || username.equals("")) {
        			return;
        		}
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	String password = null;
        	try {
        		password = (String) jsonMailConfig.get("password");
        		if(password == null || password.equals("")) {
        			return;
        		}
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	String frommail = null;
        	try {
        		frommail = (String) jsonMailConfig.get("frommail");
        		if(frommail == null || frommail.equals("")) {
        			return;
        		}
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	String tomail = null;
        	try {
        		tomail = (String) jsonMailConfig.get("tomail");
        		if(tomail == null || tomail.equals("")) {
        			return;
        		}
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	boolean smtpauth = true;
        	try {
        		smtpauth = (Boolean) jsonMailConfig.get("smtpauth");        		
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	boolean starttls = true;
        	try {
        		starttls = (Boolean) jsonMailConfig.get("starttls");        		
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	String smtphost = null;
        	try {
        		smtphost = (String) jsonMailConfig.get("smtphost");
        		if(smtphost == null || smtphost.equals("")) {
        			return;
        		}
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}
        	
        	long smtpport = 465;
        	try {
        		smtpport = (Long) jsonMailConfig.get("smtpport");        		
        	} catch(NullPointerException | ClassCastException e) {/* keep default value */}   
        }
    	catch (FileNotFoundException e1) {        	
        	return;
		} catch (IOException e1) {			
			return;
		} catch (ParseException e) {			
			return;
		}    	
	}
}
