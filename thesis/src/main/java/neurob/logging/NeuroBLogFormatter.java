package neurob.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class NeuroBLogFormatter extends Formatter {
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	public String format(LogRecord rec) {
		StringBuilder b = new StringBuilder(1000);
		
		b.append(df.format(new Date(rec.getMillis()))).append(" ");
		b.append('[').append(rec.getLevel()).append("]: ");
		b.append(formatMessage(rec)).append('\n');
		return b.toString();
	}
	

}
