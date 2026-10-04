# Generates raster launcher icons (all densities) for Rupee Splitter.
# The mark mirrors res/drawable/ic_logo.xml: a gradient squircle, a white
# rupee glyph and three decreasing portions. Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools\generate_launcher_icons.ps1
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$res  = Join-Path $root 'app\src\main\res'

$densities = [ordered]@{
    'mipmap-mdpi'    = 48
    'mipmap-hdpi'    = 72
    'mipmap-xhdpi'   = 96
    'mipmap-xxhdpi'  = 144
    'mipmap-xxxhdpi' = 192
}

function New-RoundedPath([double]$x, [double]$y, [double]$w, [double]$h, [double]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    if ($d -gt $h) { $d = $h }
    if ($d -gt $w) { $d = $w }
    $p.AddArc([single]$x, [single]$y, [single]$d, [single]$d, 180, 90)
    $p.AddArc([single]($x + $w - $d), [single]$y, [single]$d, [single]$d, 270, 90)
    $p.AddArc([single]($x + $w - $d), [single]($y + $h - $d), [single]$d, [single]$d, 0, 90)
    $p.AddArc([single]$x, [single]($y + $h - $d), [single]$d, [single]$d, 90, 90)
    $p.CloseFigure()
    return $p
}

function New-ScaledPoint([double]$x, [double]$y, [double]$s) {
    return New-Object System.Drawing.PointF([single]($x * $s), [single]($y * $s))
}

foreach ($key in $densities.Keys) {
    $size = $densities[$key]
    $dir  = Join-Path $res $key
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $s = $size / 108.0

    foreach ($round in @($false, $true)) {
        $bmp = New-Object System.Drawing.Bitmap($size, $size)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

        $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
        $c1 = [System.Drawing.ColorTranslator]::FromHtml('#5566FF')
        $c2 = [System.Drawing.ColorTranslator]::FromHtml('#2A2FD8')
        $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $c1, $c2, 45)

        if ($round) {
            $g.FillEllipse($bg, 0, 0, $size, $size)
        } else {
            $path = New-RoundedPath 0 0 $size $size ($size * 0.24)
            $g.FillPath($bg, $path)
            $path.Dispose()
        }

        $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)

        # Rupee glyph
        $g.FillRectangle($white, [single](24 * $s), [single](30 * $s), [single](34 * $s), [single](9 * $s))
        $g.FillRectangle($white, [single](24 * $s), [single](47 * $s), [single](34 * $s), [single](9 * $s))
        $g.FillRectangle($white, [single](24 * $s), [single](30 * $s), [single](9 * $s), [single](52 * $s))
        $leg = @(
            (New-ScaledPoint 33 56 $s),
            (New-ScaledPoint 40 56 $s),
            (New-ScaledPoint 56 82 $s),
            (New-ScaledPoint 47 82 $s)
        )
        $g.FillPolygon($white, [System.Drawing.PointF[]]$leg)

        # Three decreasing portions
        $bars = @(
            @(64, 34, 22, 10, 77),
            @(64, 49, 16, 10, 72),
            @(64, 64, 10, 10, 66)
        )
        foreach ($b in $bars) {
            $alpha = [int]$b[4]
            $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($alpha, 255, 255, 255))
            $bp = New-RoundedPath ($b[0] * $s) ($b[1] * $s) ($b[2] * $s) ($b[3] * $s) (4 * $s)
            $g.FillPath($brush, $bp)
            $bp.Dispose()
            $brush.Dispose()
        }

        $white.Dispose()
        $bg.Dispose()
        $g.Dispose()

        $name = if ($round) { 'ic_launcher_round.png' } else { 'ic_launcher.png' }
        $out = Join-Path $dir $name
        $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        Write-Host "wrote $out"
    }
}
