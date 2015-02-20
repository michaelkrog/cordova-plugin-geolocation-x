var exec = require("cordova/exec");
module.exports = {
    /**
    * @property {Object} stationaryRegion
    */
    stationaryRegion: null,
    /**
    * @property {Object} config
    */
    config: {},

    configure: function(success, failure, config) {
        this.config = config;
        var locationTimeout     = (config.locationTimeout >= 0) ? config.locationTimeout : 60,      // seconds
            debug               = config.debug || false,
            notificationTitle   = config.notificationTitle || "Background tracking",
            notificationText    = config.notificationText || "ENABLED";
            activityType        = config.activityType || "OTHER";
            stopOnTerminate     = config.stopOnTerminate || false;

        exec(success || function() {},
             failure || function() {},
             'GeolocationX',
             'configure',
             [locationTimeout, debug, notificationTitle, notificationText, activityType, stopOnTerminate]
        );
    },
    start: function(success, failure) {
        exec(success || function() {},
             failure || function() {},
             'GeolocationX',
             'start',
             []);
    },
    stop: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'GeolocationX',
            'stop',
            []);
    },
    finish: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'GeolocationX',
            'finish',
            []);
    },
    setMinimumDistance: function(success, failure, value) {
        exec(success || function() {},
            failure || function() {},
            'GeolocationX',
            'setMinimumDistance',
            [value]);
    },
    setMinimumInterval: function(success, failure, value) {
        exec(success || function() {},
            failure || function() {},
            'GeolocationX',
            'setMinimumInterval',
            [value]);
    },
    setPrecision: function(success, failure, value) {
        exec(success || function() {},
            failure || function() {},
            'GeolocationX',
            'setPrecision',
            [value]);
    }

};
