# Preserve the supplied master; ship only the opening and a smooth tail after emergence.
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceAudio = Join-Path $projectRoot 'assets/audio/portal-source.mp3'
$gameAudio = Join-Path $projectRoot 'common/src/main/resources/assets/celestial_nail/sounds/portal_open.ogg'
& ffmpeg -hide_banner -loglevel error -y -i $sourceAudio -t 10 -ac 1 -af 'afade=t=out:st=8:d=2' -c:a libvorbis -q:a 4 $gameAudio
if ($LASTEXITCODE -ne 0) { throw 'Portal audio conversion failed' }
