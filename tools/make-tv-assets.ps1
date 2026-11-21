# Generates the SanTube TV image assets (icon, corner logos, channel logo)
# from logo-S.png. Run from anywhere: powershell -File tools\make-tv-assets.ps1
param(
    [string]$LogoPath = "$PSScriptRoot\..\logo-S.png",
    [string]$Res = "$PSScriptRoot\..\smarttubetv\src\stsantube\res"
)

Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;
using System.Runtime.InteropServices;

public static class SanAssets {
    // Tight bounds of the non-transparent pixels
    public static Rectangle Bounds(Bitmap bmp) {
        BitmapData d = bmp.LockBits(new Rectangle(0, 0, bmp.Width, bmp.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        byte[] px = new byte[d.Stride * d.Height];
        Marshal.Copy(d.Scan0, px, 0, px.Length);
        bmp.UnlockBits(d);
        int minX = bmp.Width, minY = bmp.Height, maxX = -1, maxY = -1;
        for (int y = 0; y < bmp.Height; y++) {
            for (int x = 0; x < bmp.Width; x++) {
                if (px[y * d.Stride + x * 4 + 3] > 8) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                }
            }
        }
        return Rectangle.FromLTRB(minX, minY, maxX + 1, maxY + 1);
    }

    public static Graphics Canvas(Bitmap bmp) {
        Graphics g = Graphics.FromImage(bmp);
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
        g.PixelOffsetMode = PixelOffsetMode.HighQuality;
        g.CompositingQuality = CompositingQuality.HighQuality;
        g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
        return g;
    }

    // Draws the logo (cropped to src) centred in box, as large as fits
    public static void DrawLogo(Graphics g, Bitmap logo, Rectangle src, RectangleF box, ImageAttributes attrs) {
        float scale = Math.Min(box.Width / src.Width, box.Height / src.Height);
        float w = src.Width * scale, h = src.Height * scale;
        PointF[] dest = {
            new PointF(box.X + (box.Width - w) / 2, box.Y + (box.Height - h) / 2),
            new PointF(box.X + (box.Width - w) / 2 + w, box.Y + (box.Height - h) / 2),
            new PointF(box.X + (box.Width - w) / 2, box.Y + (box.Height - h) / 2 + h)
        };
        g.DrawImage(logo, dest, src, GraphicsUnit.Pixel, attrs);
    }

    public static GraphicsPath RoundRect(RectangleF r, float radius) {
        float d = radius * 2;
        GraphicsPath p = new GraphicsPath();
        p.AddArc(r.X, r.Y, d, d, 180, 90);
        p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
        p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
        p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
        p.CloseFigure();
        return p;
    }

    // Keeps the alpha, replaces the color
    public static ImageAttributes Tint(Color c) {
        ColorMatrix m = new ColorMatrix(new float[][] {
            new float[] {0, 0, 0, 0, 0},
            new float[] {0, 0, 0, 0, 0},
            new float[] {0, 0, 0, 0, 0},
            new float[] {0, 0, 0, 1, 0},
            new float[] {c.R / 255f, c.G / 255f, c.B / 255f, 0, 1}
        });
        ImageAttributes a = new ImageAttributes();
        a.SetColorMatrix(m);
        return a;
    }
}
'@

$surface = [System.Drawing.ColorTranslator]::FromHtml('#141213')
$onSurface = [System.Drawing.ColorTranslator]::FromHtml('#EFE0DE')

$logo = New-Object System.Drawing.Bitmap $LogoPath
$src = [SanAssets]::Bounds($logo)
"logo bounds: $src"

New-Item -ItemType Directory -Force "$Res\mipmap-nodpi", "$Res\drawable" | Out-Null

function Save-Png($bmp, $path) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    "{0,7} bytes  {1}" -f (Get-Item $path).Length, $path
}

# --- app_icon.png: 320x320, dark rounded square with the S (same idea as the phone icon)
$bmp = New-Object System.Drawing.Bitmap 320, 320, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [SanAssets]::Canvas($bmp)
$path = [SanAssets]::RoundRect((New-Object System.Drawing.RectangleF 0, 0, 320, 320), 68)
$brush = New-Object System.Drawing.SolidBrush $surface
$g.FillPath($brush, $path)
[SanAssets]::DrawLogo($g, $logo, $src, (New-Object System.Drawing.RectangleF 62, 62, 196, 196), $null)
$g.Dispose()
Save-Png $bmp "$Res\mipmap-nodpi\app_icon.png"

# --- generic_channels.png: 300x300, full-bleed square (logo of the home screen channels)
$bmp = New-Object System.Drawing.Bitmap 300, 300, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [SanAssets]::Canvas($bmp)
$g.Clear($surface)
[SanAssets]::DrawLogo($g, $logo, $src, (New-Object System.Drawing.RectangleF 62, 62, 176, 176), $null)
$g.Dispose()
Save-Png $bmp "$Res\drawable\generic_channels.png"

# --- app_logo*.png: 180x180, the S alone on transparent (badge in the corner of the browse screen)
foreach ($variant in @(@{ name = 'app_logo'; tint = $null }, @{ name = 'app_logo_semi_red'; tint = $null }, @{ name = 'app_logo_semi_grey'; tint = '#B8B0AF' })) {
    $bmp = New-Object System.Drawing.Bitmap 180, 180, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [SanAssets]::Canvas($bmp)
    $attrs = $null
    if ($variant.tint) { $attrs = [SanAssets]::Tint([System.Drawing.ColorTranslator]::FromHtml($variant.tint)) }
    [SanAssets]::DrawLogo($g, $logo, $src, (New-Object System.Drawing.RectangleF 14, 14, 152, 152), $attrs)
    $g.Dispose()
    Save-Png $bmp "$Res\mipmap-nodpi\$($variant.name).png"
}

# mipmap-nodpi\app_banner.png (640x360, the tile on the TV home screen) is NOT generated:
# it is a hand-made image with the blackletter wordmark. Replace the file itself to change it.

$logo.Dispose()
