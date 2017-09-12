# cfggen

## cfggen作为配置工具，已经集成到perfect(https://github.com/pirunxi/perfect) 框架中，后续的维护将在perfect项目中进行，此仓库不再维护。

## cfggen是一个游戏专用配置生成工具.
	
	cfggen读取excel、xml或lua文件,导出配置数据,并且生成读取配置数据所需的代码.
	cfggen良好支持本地化，支持字符串的本地化映射，比如制作繁体版，仅仅半小时就能轻松简单搞定
	cfggen目前支持java,csharp,lua,c++.
	cfggen经受过项目实践，非常稳定强大可靠。

## cfggen有如下优点
* 完备

		支持bool,int,long,string,float,struct,enum,list,set,map数据类型。支持多态数据。可以优雅表达任何简单或复杂游戏
		配置。也就是说，利用cfggen可以自动处理游戏内所有配置,不需要程序写一行导出或者加载代码。配置非常灵活，策划可以
		按照最自然的方式去配置数据，不需要合并单元格或者拆表之类的奇技淫巧。生成简洁的适合游戏内使用的配置加载代码。

* 良好的模块化

		模块化管理定义与数据文件,生成模块化的代码。

* 支持丰富的数据源类型

		支持excel族(csv,xls,xlsx,xlsm等等)数据文件，支持excel多标签页。
		支持xml配置文件。
		支持lua配置文件。
		以上几种形式的配置可以等效替换，几乎无限制策划如何配置数据。
		支持在xml里定义表,也支持直接在excel里定义表(extern表)。

* 强大的数据检查功能

		支持策划数据类型合法性检查
		支持引用检查(ref,类似于mysql的外键)
		支持路径引用检查(refpath, 配置里经常有图片或者模型的相对路径，用于检查资源缺失尤其有效)，
		支持多重索引(index)，支持数据id唯一性检查

* 选择性导出

		可以在表级别，字段级别指定分组(group),选择性导出服务器和客户端的配置。

* 支持最常用的语言

		java,c#,lua,c++,添加新语言支持也很容易

* 统一的导出数据格式

		只有一种简洁的基本行的文本数据格式，对svn友好，很容易版本对比，在使用excel数据源尤其有用。

* 为编辑器提供序列化支持

		一些极复杂的配置不适合直接在excel编辑，一般使用编辑器来制作。cfggen能够为编辑器
		生成版本兼容的序列化代码，序列化后的xml数据能够被cfggen与excel数据同等处理。
* 支持本地化		
		
		通过标注本地化字段，以额外的字符串映射的方式，巧妙实现不必修改原excel或者xml文件，就能实现配置的本地化。
		制作各种本地化版本都非常容易，尤其繁体版，寥寥几分钟就可搞定。
##  cfggen使用

		csv目录是一个实际项目中抽取出的展示cfggen所有功能的示例配置,配置由定义文件(xml)与数据文件(excel或者xml)组成。
		code目录是由csv生成的java配置加载
		data目录是由csv生成的最终导出数据(使用了all分组,如果使用其他分组,导出的数据有所差别)
		
* cfggen.jar与gencfg.bat
 
		cfggen.jar是最新build的jar包
		gencfg.bat是导出配置与生成配置代码的示例脚本
* cfg/cfg.xml
 
		定义根文件
* cfg/role

		profession2表 典型extern表，结构与profession相同,但它直接在excel里定义表结构。
		profession表 经典的规范表的例子,在游戏配置中最常见
		roleconfig表是 单键表的例子
		name表 是多标签页的单键表
* cfg/item

		item表是 多态表的经典例子(每行有公共字段,但根据物品类型有各自独有字段)
		itemconfig表 是单键表例子
* cfg/cmd 和 cfg/currency

		全局性的一些定义.不具体到某个表
* cfg/ectype

		storylayout表是规范表,但它的某些子孙字段是非常复杂的多态类型。策划手动配置这种数据比较困难，需要借助编辑器。
		cfggen为编辑器提供了序列化支持,导出的xml数据能被cfggen识别处理。

