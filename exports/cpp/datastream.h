#ifndef __DATA_STREAM_HPP__
#define __DATA_STREAM_HPP__
#include <stdint.h>
#include <string>
#include <vector>
#include <map>
#include <set>

#include <cstdlib>
#include <fstream>
#include <iostream>

namespace cfg {
	class Object;

	class Error{
	public:
		const std::string content;
		const std::string type;
		Error(const std::string& content, const std::string& type) : content(content), type(type) {
		}
	};

	class DataStream {
	public:
		static DataStream* create(const std::string& dataFile) {
			std::ifstream fin(dataFile.c_str());
			if(!fin)
				throw Error(dataFile, "open fail");
			std::vector<std::string> lines;
			std::string line;
			while(std::getline(fin, line)) {
				lines.push_back(line);
			}
			return create(lines);
		}

		static DataStream* create(std::vector<std::string>& inputDatas) {
			return new DataStream(inputDatas);
		}

		bool getBool() {
			const std::string& str = getNext();
			if(str == "true")
				return true;
			else if(str == "false")
				return false;
			else
				throw Error(str, "bool");
		}

		int32_t getInt() {
			const std::string& str = getNext();
			char* ptrEnd;
			int x  = std::strtol(str.c_str(), &ptrEnd, 10);
			if(*ptrEnd)
				throw Error(str, "int");
			return x;
		}

		int64_t getLong() {
			const std::string& str = getNext();
			char* ptrEnd;
			int64_t x  = std::strtoll(str.c_str(), &ptrEnd, 10);
			if(*ptrEnd)
				throw Error(str, "long");
			return x;
		}

		float getFloat() {
			const std::string& str = getNext();
			const char* ptrStart = str.c_str();
			char* ptrEnd;
			float x  = std::strtof(ptrStart, &ptrEnd);
			if(*ptrEnd)
				throw Error(str, "float");
			return x;
		}

		std::string getString() {
			return getNext();
		}

		int32_t getSize() {
			const std::string& str = getNext();
			char* ptrEnd;
			int32_t x  = std::strtol(str.c_str(), &ptrEnd, 10);
			if(x < 0 || *ptrEnd)
				throw Error(str, "size");
			return x;
		}

		Object* getObject(const std::string& name) {
			Factory* factory = getFactorys()[name];
			if(factory)
				return factory->create(*this);
			throw Error(name, "unknown object type");
		}

		template<typename T>
		static void registerType(const std::string& name) {
			getFactorys()[name] = new FactoryImp<T>();
		}

	private:
		DataStream(std::vector<std::string>& inputDatas) : datas(inputDatas), index(0) {

		}

		const std::string& getNext() {
			if(index < datas.size()) {
				return datas[index++];
			} else {
				throw Error("read not enough", "");
			}
		}

		const std::vector<std::string> datas;
		size_t index;
		class Factory {
		public:
			virtual Object* create(DataStream& ds) = 0;
		};

		template<typename T>
		class FactoryImp : public Factory {
		public:
			Object* create(DataStream& ds) {
				return new T(ds);
			}
		};

		static std::map<std::string, Factory*>& getFactorys() {
			static std::map<std::string, Factory*> factorys;
			return factorys;
		}
	};
}

#endif
