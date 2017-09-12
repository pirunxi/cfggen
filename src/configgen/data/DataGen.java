package configgen.data;

import configgen.Generator;
import configgen.Main;
import configgen.type.Config;

public class DataGen implements Generator {

	@Override
	public void gen() {
		for(Config cfg : Config.configs.values()) {
			cfg.save(Main.groups);
		}
	}

}
