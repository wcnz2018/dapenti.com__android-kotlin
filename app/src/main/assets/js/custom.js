(function() {
    $('video').mediaelementplayer({
        videoWidth: '100%',
        videoHeight: '100%',
        hideVideoControlsOnLoad: true,
        pauseOtherPlayers: true,
        enableAutosize: true
    })
    console.log("Yo, custom.js loaded!")
}) ()
