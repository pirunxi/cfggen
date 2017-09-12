#include <iostream>
#include "cfg/cfgmgr.h"

cfg::CfgMgr config;

int main(int argc, char** argv) {
	try {
		config.load(argv[1]);
	} catch(cfg::Error& e) {
		std::cout << "exception:" << e.content << "," << e.type << std::endl;
	}
	std::cout << "end.." << std::endl;
	return 0;
}
