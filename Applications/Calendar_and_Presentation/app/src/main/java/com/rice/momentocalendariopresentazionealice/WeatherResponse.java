package com.rice.momentocalendariopresentazionealice;

public class WeatherResponse {
    public Coord coord;
    public Weather[] weather;
    public Main main;
    public Wind wind;
    public Clouds clouds;
    public Sys sys;
    public String name;

    public class Coord {
        public double lon;
        public double lat;
    }

    public class Weather {
        public int id;
        public String main;
        public String description;
        public String icon;
    }

    public class Main {
        public double temp;
        public double feels_like;
        public double temp_min;
        public double temp_max;
        public int pressure;
        public int humidity;
    }

    public class Wind {
        public double speed;
        public int deg;
    }

    public class Clouds {
        public int all;
    }

    public class Sys {
        public String country;
        public long sunrise;
        public long sunset;
    }
}
