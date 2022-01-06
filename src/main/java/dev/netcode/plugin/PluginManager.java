package dev.netcode.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import dev.netcode.util.LogLevel;
import dev.netcode.util.StringUtils;

@SuppressWarnings("exports")
public class PluginManager {
	
	private BiConsumer<LogLevel, String> logFunction;
	private ArrayList<Plugin> plugins;
	
	public PluginManager(BiConsumer<LogLevel, String> logFunction) {
		plugins = new ArrayList<Plugin>();
		this.logFunction = logFunction;
	}
	
	public void start() {
		if(!new File("plugins").exists()) {
			new File("plugins").mkdir();
		}
		File[] files = new File("plugins").listFiles();
		for(File f:files) {
			loadPlugin(f);
		}
		for(Plugin p:plugins) {
			p.start();
		}
		logFunction.accept(LogLevel.INFO, plugins.size()+" Plugin(s) loaded!");
	}

	@SuppressWarnings({ "rawtypes", "resource", "unchecked" })
	public void loadPlugin(File file) {
		if(StringUtils.getFileExtension(file).equalsIgnoreCase("jar")) {
			try {
				JarFile f = new JarFile("plugins"+File.separator+file.getName());
				Manifest manifest = f.getManifest();
				Attributes attrib = manifest.getMainAttributes();
				String main = attrib.getValue(Attributes.Name.MAIN_CLASS);
				Class cl = new URLClassLoader(new URL[]{file.toURI().toURL()}).loadClass(main);
				Class[] interfaces = cl.getInterfaces();
				boolean isPlugin = false;
				for(var intf : interfaces) {
					if(intf.getName().contentEquals("dev.netcode.plugin.Plugin")) {
						isPlugin = true;
					}
				}
				if(isPlugin) {
					Plugin plugin = (Plugin) cl.getConstructor().newInstance();
					plugins.add(plugin);
				}
				f.close();
			} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				logFunction.accept(LogLevel.ERROR, "Error while loading Plugin: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public int getPluginCount() {
		return plugins.size();
	}

	public void stop() {
		for(var plugin : plugins) {
			plugin.stop();
		}
	}

	public void clearPlugins() {
		plugins.clear();
	}
	
}
