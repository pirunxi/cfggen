#ifndef __CFG_OBJECT_H__
#define __CFG_OBJECT_H__
namespace cfg {
class Object {
public:
	virtual int getTypeId() = 0;
	virtual ~Object() {}
};

}
#endif
