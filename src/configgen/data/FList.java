package configgen.data;

import configgen.FlatStream;
import configgen.Main;
import configgen.RowColumnStream;
import configgen.Utils;
import configgen.type.Field;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FList extends Type {
	public final List<Type> values = new ArrayList<Type>();
	public final HashMap<String, HashSet<Type>> indexs = new HashMap<String, HashSet<Type>>();
	public FList(FStruct host, Field define) {
		super(host, define);
		for(String idx : define.getIndexs()) {
			indexs.put(idx, new HashSet<Type>());
		}
	}

	public void addValue(Type value) {
		if(host == null) {
			Main.addLastLoadData(value);
		}
		values.add(value);
		for(String idx : define.getIndexs()) {
			final HashSet<Type> m = indexs.get(idx);
			FStruct s = (FStruct)value;
			Type key = s.getField(idx);
			if(!m.add(key))
				throw new RuntimeException(String.format("field:%s idx:%s key:%s duplicate!", define, idx, key));
		}
	}

    public void loadMultiRecordNotCheckEnd(FlatStream is) {
        final Field valueDefine = define.getValueFieldDefine();
        while(!is.isSectionEnd()) {
            addValue(Type.create(host, valueDefine, is));
        }
    }

	public void loadMultiRecord(FlatStream is) {
		final Field valueDefine = define.getValueFieldDefine();
		while(!is.isSectionEnd()) {
			addValue(Type.create(host, valueDefine, is));
		}
        expectEnd(is);
	}

	private void expectEnd(FlatStream is) {
        boolean hasRemain = false;
        try {
            is.getString();
            hasRemain = true;
        } catch (Exception e) {

        }
        if(hasRemain)
            throw new RuntimeException("有部分未读数据,可能是错误地提前输入了列表结束符 ]] !");
        // 读完所有数据后应该不再有有效数据.
    }

	public void loadMultiRecord(Element ele) {
        Field valueDefine = define.getValueFieldDefine();
		final NodeList nodes = ele.getChildNodes();
		for(int i = 0, n = nodes.getLength() ; i < n ; i++) {
			final Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				addValue(Type.create(host, valueDefine, (Element)node));
			}
		}
	}

	public void loadMultiRecord(LuaValue g) {
		Field valueDefine = define.getValueFieldDefine();
		for(int i = 1, n = g.rawlen() ; i <= n ; i++)
			addValue(Type.create(host, valueDefine, g.get(i)));
	}

	@SuppressWarnings("unchecked")
	public void loadOneRecord(File file) throws Exception {
		Field valueDefine = define.getValueFieldDefine();
		try {
		    final Object data = Utils.parseAsXmlOrLuaOrFlatStream(file.getAbsolutePath());
		    if(data instanceof  Element) {
		        addValue(Type.create(host, valueDefine, (Element)data));
            } else if(data instanceof List){
                final FlatStream is = new RowColumnStream((List<List<String>>)data);
                addValue(Type.create(host, valueDefine, is));
                expectEnd(is);
            } else {
				addValue(Type.create(host, valueDefine, (LuaValue)data));
			}
		} catch (Exception e) {
			System.out.printf("【加载文件失败】 %s%n", file.getAbsolutePath());
            throw e;
        }
	}

	public void loadOneRecord(LuaTable g) throws Exception {
		addValue(Type.create(host, define.getValueFieldDefine(), g));
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("List<").append(define.getFullType()).append(">{");
		values.forEach(v -> sb.append(v).append(","));
		sb.append("}");
		return sb.toString();
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public void verifyData() {
        for (Type d : values) {
            if (host == null) {
                Main.setCurVerifyData(d);
            }
            d.verifyData();
        }
    }

	@Override
	public boolean isNull() {
		return false;
	}

}
