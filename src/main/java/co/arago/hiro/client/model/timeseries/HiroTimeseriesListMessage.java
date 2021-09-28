package co.arago.hiro.client.model.timeseries;

import co.arago.hiro.client.model.HiroItemListMessage;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HiroTimeseriesListMessage extends HiroItemListMessage<HiroTimeseriesListMessage.TimeseriesEntry> {

    private static final long serialVersionUID = 5549447662955430498L;

    public static class TimeseriesEntry extends HiroMessage {
        /**
         * 
         */
        private static final long serialVersionUID = 9194337636660014261L;
        public String value;
        public Long timestamp;

        @JsonCreator
        public TimeseriesEntry(
                @JsonProperty("value") String value,
                @JsonProperty("timestamp") Long timestamp
        ) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public static TimeseriesEntry create(Object object) {
            return JsonUtil.DEFAULT.transformObject(object, TimeseriesEntry.class);
        }
    }

    public static HiroTimeseriesListMessage create(Object object) {
        return JsonUtil.DEFAULT.transformObject(object, HiroTimeseriesListMessage.class);
    }
}
