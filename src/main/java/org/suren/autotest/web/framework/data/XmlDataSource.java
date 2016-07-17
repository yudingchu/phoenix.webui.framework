/**
 * http://surenpi.com
 */
package org.suren.autotest.web.framework.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.apache.poi.util.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.VisitorSupport;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.suren.autotest.web.framework.baidu.BaiduHomePage;
import org.suren.autotest.web.framework.core.ui.Text;
import org.suren.autotest.web.framework.page.Page;

/**
 * @author suren
 * @date Jul 17, 2016 8:56:51 AM
 */
@Component("xml_data_source")
public class XmlDataSource implements DataSource
{
	private Page page;

	public boolean loadData(DataResource resource, Page page)
	{
		this.page = page;
		URL url = resource.getUrl();
		if(url == null)
		{
			return false;
		}
		
		InputStream inputStream = null;
		try
		{
			inputStream = url.openStream();
			
			parse(inputStream);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (DocumentException e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(inputStream);
		}
		
		return false;
	}

	/**
	 * @param inputStream
	 * @throws DocumentException 
	 */
	private void parse(InputStream inputStream) throws DocumentException
	{
		Document document = new SAXReader().read(inputStream);

		parse(document);
	}
	
	private void parse(Document doc)
	{
		String pageClass = page.getClass().getName();
		List<Element> dataSourceList = doc.selectNodes(
				String.format("/dataSources/dataSource[@pageClass='%s']/page", pageClass));
		if (dataSourceList == null)
		{
			throw new RuntimeException("can not found datasource config.");
		}
		
		for(Element dataSource : dataSourceList)
		{
			dataSource.accept(new VisitorSupport()
			{

				@Override
				public void visit(Element node)
				{
					if(!"field".equals(node.getName()))
					{
						return;
					}
					
					String fieldName = node.attributeValue("name");
					String value = node.attributeValue("data");

					Method getterMethod = BeanUtils.findMethod(page.getClass(),
							"get" + fieldName.substring(0, 1).toUpperCase()
									+ fieldName.substring(1));
					
					try
					{
						Object eleObj = getterMethod.invoke(page);
						if(!(eleObj instanceof Text))
						{
							return;
						}
						
						Text text = (Text) eleObj;
						text.setValue(value);
					}
					catch (IllegalAccessException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IllegalArgumentException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (InvocationTargetException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	public static void main(String[] args)
	{
		Page page = new BaiduHomePage();
		XmlDataSource data = new XmlDataSource();
		data.loadData(new DataResource()
		{
			
			public URL getUrl()
			{
				return DataResource.class.getClassLoader().
						getResource("dataSource/baidu_website_datasource.xml");
			}
		}, page);
		
		System.out.println(page);
	}

}