package configgen;

import configgen.data.DataGen;
import configgen.type.Config;
import configgen.type.ENUM;
import configgen.type.Group;
import configgen.type.Struct;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public final class Main {
	public static String xmlSchemeFile = "";
	public static String csvDir = "";
	public static String codeDir = "";
	public static String dataDir = "";
	public static String csmarshalcodeDir = "";
	public static String outputEncoding = "utf8";
	public static String inputEncoding = "GB2312";
	public static boolean verbose = false;
	public static boolean check = false;
	public static String cfgmgrName = "CfgMgr";
	public static String inputLocalizedFile = null;
	public static String outputLocalizedFile = null;
	public static String outputUnlocalizedFile = null;

	public static final String magicStringForNewLine = ".g9~/";
	
	public static final Set<String> languages = new HashSet<String>();
	public static final Set<String> groups = new HashSet<String>();

	
    private static void usage(String reason) {
        System.out.println(reason);

        System.out.println("Usage: java -jar config.jar [options]");
        System.out.println("    -lan cs:lua:java     language type. can be multi.");
        System.out.println("    -configxml       config xml file");
        System.out.println("    -codedir         output code directory.");
        System.out.println("    -datadir output data directory");
        System.out.println("    -csmarshalcodedir   csharp marshal code output directory" );
        System.out.println("    -group server:client:all:xxx   group to export, can be multi.");
        System.out.println("    -outputencoding  output encoding. default utf8");
        System.out.println("    -inputencoding   input csv encoding. default GB2312");
        System.out.println("    -verbose  show detail. default not");
        System.out.println("    -check load and check even not set -datadir");
		System.out.println("    -localized  inputlocalizedfile:outputlocalizedfile:outputunlocalizedfile     set input&output localized file");
        System.out.println("    -cfgmgrname set cfgmgr class name");
        System.out.println("    --help show usage");

        Runtime.getRuntime().exit(1);
    }
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-lan":
				languages.addAll(Arrays.asList(args[++i].split(":")));
				break;
			case "-configxml":
				xmlSchemeFile = args[++i];
				break;
			case "-codedir":
				codeDir = args[++i];
				break;
			case "-csmarshalcodedir":
				csmarshalcodeDir = args[++i];
				break;
			case "-datadir":
				dataDir = args[++i];
				break;
			case "-group":
				groups.addAll(Arrays.asList(args[++i].split(":")));
				break;
			case "-outputencoding":
				outputEncoding = args[++i];
				break;
			case "-inputencoding":
				inputEncoding = args[++i];
				break;
			case "-verbose":
				verbose = true;
				break;
			case "-check":
				check = true;
				break;
			case "-localized": {
				final String[] params = args[++i].split(":");
				if(params.length != 3)
					usage("-localized");
				inputLocalizedFile = params[0];
				outputLocalizedFile = params[1];
				outputUnlocalizedFile = params[2];
				break;
			}
			case "-cfgmgrname":
				cfgmgrName = args[++i];
				break;
			case "--help":
				usage("");
				break;
			default:
				usage("unknown args " + args[i]);
				break;
			}
		}

		if(xmlSchemeFile.isEmpty())
			usage("-configxml miss");
		if(csmarshalcodeDir.isEmpty() && groups.isEmpty())
			usage("-group miss");
		if(codeDir.isEmpty() && !languages.isEmpty())
			usage("-codedir miss");
		
		if(codeDir.isEmpty() && dataDir.isEmpty() && csmarshalcodeDir.isEmpty() && !check)
			usage("needs -codedir or -datadir or csmarshalcodedir or -check");

        final long startTime = System.currentTimeMillis();
        final File cfgxml = new File(xmlSchemeFile);
        final Path parent = cfgxml.toPath().getParent();
        csvDir = parent != null ? parent.toString() : ".";
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cfgxml);
        Element root = doc.getDocumentElement();
        
        curXml = xmlSchemeFile;
        loadDefine(doc, root, "");
        dumpDefine();
        verifyDefine();
        
        if(!codeDir.isEmpty() && !languages.isEmpty()) {
        	// lua版代码就两个文件,特殊处理不删目录
        	if(!languages.contains("lua")) {
            	Utils.deleteDirectory(codeDir);
        	}
	        for(String lan : languages) {
				Class<?> cls = Class.forName("configgen.lans." + lan + ".CodeGen");
				Generator generator = (Generator) cls.newInstance();
				generator.gen();
	        }
        }
        
        if(!csmarshalcodeDir.isEmpty()) {
        	Utils.deleteDirectory(csmarshalcodeDir);
        	(new configgen.lans.cs.CodeGen()).genMarshallCode();
        }
        
        if(!dataDir.isEmpty() || check) {
	        try {
	        	loadData();
	        	verifyData();
	        } catch(Exception e) {
				System.out.println();
	        	System.out.println("=================last datas=====================");
	        	System.out.println(lastLoadData);
	        	System.out.println("=================last datas=====================");
	        	e.printStackTrace();
	        	System.exit(1);
	        }

	        if(!dataDir.isEmpty()) {
                Utils.deleteDirectory(dataDir);
	        	new DataGen().gen();
	        }
        }
        final long endTime = System.currentTimeMillis();
        System.out.printf("%n%n");
        System.out.printf("====> cost time %.2f s <====%n", (endTime - startTime) / 1000.0);
	}

	public static String curXml = "";
	public static void loadDefine(Document doc, Element root, String relateDir) throws Exception {
		final String namespace = root.getAttribute("namespace");
		if(namespace.isEmpty())
			Utils.error("xml:%s configs's attribute<namespace> missing", curXml);
        for(Element ele : Utils.getChildsByTagName(root, "group")) {
        	Group.load(ele);
        }
        
        for(Element ele : Utils.getChildsByTagName(root, "enum")) {
        	new ENUM(namespace, ele);
        }
        
        for(Element ele : Utils.getChildsByTagName(root, "struct")) {
        	new Struct(namespace, ele);
        }
  
        for(Element ele : Utils.getChildsByTagName(root, "config")) {
			if("true".equals(ele.getAttribute("extern"))) {
				if(!Utils.getChildsByTagName(ele, "field").isEmpty()) {
					Utils.error("extern config:" + ele.getAttribute("name") + " can't define fields");
				}
				Struct.importDefineFromInput(doc, ele, relateDir, ele.getAttribute("input"));
			}
        	new Struct(namespace, ele);
        	new Config(namespace, ele, relateDir);
        }

        for(Element ele : Utils.getChildsByTagName(root, "namespace")) {
            final String name = ele.getAttribute("name");
            if(name.isEmpty())
                Utils.error("xml:%s subnamespace's name missing!", root);
            ele.setAttribute("namespace", namespace + "." + name);
            loadDefine(doc, ele, relateDir);
        }
        
        for(Element ele : Utils.getChildsByTagName(root, "import")) {
        	for(String file : Utils.split(ele, "input")) {
        		final String oldXml = curXml;
        		curXml = file;
        		final String newRelateDir = file.contains("/") ? Utils.combine(relateDir, file.substring(0, file.lastIndexOf('/'))) : relateDir;
				final Document subDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Utils.combine(csvDir, relateDir) + "/" + file);
        		loadDefine(subDoc, subDoc.getDocumentElement(), newRelateDir);
        		curXml = oldXml;
        	}
        }
        
	}
	
	public static void println(Object s) {
		if(verbose) {
			System.out.println(s);
		}
	}
	
	public static void dumpDefine() {
		println("groups:" + Group.groups);
		Config.configs.values().forEach(c -> println(c.toString()));
		Struct.getStructs().values().forEach(s -> println(s.toString()));	
	}
	
	private static void verifyDefine() {
		Struct.getStructs().values().stream().forEach(Struct::verityDefine);
		Config.configs.values().stream().forEach(Config::verifyDefine);
	}

	static void loadData() throws Exception{
		if(inputLocalizedFile != null)
			Localized.Ins.load(inputLocalizedFile);
		/*
		Config.configs.values().parallelStream().forEach(c -> {
			try {
				lastLoadData = null;
				System.out.printf(".");
				final long t1 = System.currentTimeMillis();
				c.loadData();
				final long t2 = System.currentTimeMillis();
				if (t2 - t1 > 1000) {
					System.out.printf("%nload config:%s cost time:%.2f s%n", c.getName(), (t2 - t1) / 1000.0);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		});
		*/
		CountDownLatch countdown = new CountDownLatch(Config.configs.size());

        for (Config c : Config.configs.values()) {
			Thread work = new Thread(() -> {
				try {
					lastLoadData = null;
					System.out.printf(".");
					final long t1 = System.currentTimeMillis();
					c.loadData();
					final long t2 = System.currentTimeMillis();
					if (t2 - t1 > 1000) {
						System.out.printf("%nload config:%s cost time:%.2f s%n", c.getName(), (t2 - t1) / 1000.0);
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				} finally {
					countdown.countDown();
				}
			});
			work.start();
		}
		countdown.await();

		if(outputLocalizedFile != null)
			Localized.Ins.saveLocalizedAs(outputLocalizedFile);

		if(outputUnlocalizedFile != null)
			Localized.Ins.saveUnLocalizedAs(outputUnlocalizedFile);
	}
	
	private static void verifyData() {
		System.out.println();
		Config.configs.values().stream().forEach(Config::verifyData);
	}

    private static Object lastLoadData = null;

    private static Config curVerifyConfig = null;
    private static Object curVerifyData = null;

    public static Config getCurVerifyConfig() {
        return curVerifyConfig;
    }

    public static void setCurVerifyConfig(Config curVerifyConfig) {
        Main.curVerifyConfig = curVerifyConfig;
    }

    public static Object getCurVerifyData() {
        return curVerifyData;
    }

    public static void setCurVerifyData(Object curVerifyData) {
        Main.curVerifyData = curVerifyData;
    }

    public static void addLastLoadData(Object data) {
		lastLoadData = data;
	}

}
