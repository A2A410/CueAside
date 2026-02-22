package foz.cueaside.aa;

import java.util.List;

public class Routine {
    public String id;
    public List<AppInfo> apps;
    public String cond; // "launched", "used", "exiting"
    public int dur;
    public String unit; // "m", "h", "s"
    public String timeMode; // "session", "total"
    public IconInfo icon;
    public String title;
    public String msg;
    public boolean bubble;
    public boolean enabled;

    public static class AppInfo {
        public String name;
        public String pkg;
        public String icon; // base64 or emoji
    }

    public static class IconInfo {
        public String type; // "app", "preset", "lib"
        public String pkg;
        public String e;
        public String src;
    }
}
