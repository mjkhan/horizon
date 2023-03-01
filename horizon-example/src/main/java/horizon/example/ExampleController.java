package horizon.example;

import java.beans.PropertyEditorSupport;
import java.util.Date;

import javax.annotation.Resource;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import com.fasterxml.jackson.databind.ObjectMapper;

import horizon.base.AbstractComponent;

public class ExampleController extends AbstractComponent {
	@Resource(name="objectMapper")
	private ObjectMapper objectMapper;

	protected String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	protected <T> T fromJson(String json, Class<T> klass) {
		try {
			return objectMapper.readValue(json, klass);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	@InitBinder
	public void registerDateEditor(WebDataBinder dataBinder) {
		dataBinder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (isEmpty(text))
					setValue(null);
				else {
					if (!text.matches("^\\d+$"))
						throw new IllegalArgumentException(text);

					setValue(new Date(Long.valueOf(text)));
				}
			}
		});
	}
}