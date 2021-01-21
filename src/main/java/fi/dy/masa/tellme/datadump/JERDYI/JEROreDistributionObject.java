package fi.dy.masa.tellme.datadump.JERDYI;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JEROreDistributionObject {
    private String block;
    private String distrib;
    private boolean silktouch;
    private List<DropObject> dropsList;
    private String dim;

    class DropObject {
        private String itemStack;
        private FortuneMapping fortunes;

        class FortuneMapping {
            @SerializedName("0")
            private String zero;
            @SerializedName("1")
            private String one;
            @SerializedName("2")
            private String two;
            @SerializedName("3")
            private String three;

            public FortuneMapping(String zero, String one, String two, String three) {
                this.zero = zero;
                this.one = one;
                this.two = two;
                this.three = three;
            }
        }

        public DropObject(String itemStack, FortuneMapping fortunes) {
            this.itemStack = itemStack;
            this.fortunes = fortunes;
        }
    }

    public JEROreDistributionObject(String block, String distrib, boolean silktouch, List<DropObject> dropsList,
            String dim) {
        this.block = block;
        this.distrib = distrib;
        this.silktouch = silktouch;
        this.dropsList = dropsList;
        this.dim = dim;
    }

}
