#version 150

uniform vec4 ColorModulator;
// Половина размера фигуры в GUI-пикселях.
uniform vec2 HalfSize;
// Радиусы углов: x = низ-право, y = верх-право, z = низ-лево, w = верх-лево.
uniform vec4 Radii;
// x = толщина обводки (0 = заливка), y = мягкость края (радиус размытия тени, 0 = обычное сглаживание).
uniform vec2 Params;

// Градиент. GradMode: x = вид (0 — цвета вершин, 1 — линейный, 2 — радиальный, 3 — конический),
// y = число опор, z = пространство смешивания (0 sRGB, 1 linear, 2 OKLab, 3 OKLCH, 4 HSV), w = повторение.
uniform vec4 GradMode;
// Геометрия в пикселях относительно центра (см. ShapePaint).
uniform vec4 GradGeom;
// x = сдвиг параметра.
uniform vec4 GradExtra;
// Цвета опор 0..3 и 4..7: столбец i — RGBA опоры.
uniform mat4 GradColorsA;
uniform mat4 GradColorsB;
// Позиции опор 0..3 и 4..7.
uniform vec4 GradStopsA;
uniform vec4 GradStopsB;

in vec2 localPos;
in vec4 vertexColor;

out vec4 fragColor;

const float PI = 3.14159265;

// Signed distance до скруглённого прямоугольника (Inigo Quilez).
float roundedBoxSdf(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

// ---------------------------------------------------------------- цветовые пространства

vec3 toLinear(vec3 c) {
    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c));
}

vec3 toSrgb(vec3 c) {
    c = max(c, vec3(0.0));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c));
}

vec3 linearToOklab(vec3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    vec3 lms = sign(vec3(l, m, s)) * pow(abs(vec3(l, m, s)), vec3(1.0 / 3.0));
    return vec3(
        0.2104542553 * lms.x + 0.7936177850 * lms.y - 0.0040720468 * lms.z,
        1.9779984951 * lms.x - 2.4285922050 * lms.y + 0.4505937099 * lms.z,
        0.0259040371 * lms.x + 0.7827717662 * lms.y - 0.8086757660 * lms.z);
}

vec3 oklabToLinear(vec3 c) {
    float l = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;
    float m = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;
    float s = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;
    l = l * l * l;
    m = m * m * m;
    s = s * s * s;
    return vec3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s);
}

vec3 rgbToHsv(vec3 c) {
    float mx = max(c.r, max(c.g, c.b));
    float mn = min(c.r, min(c.g, c.b));
    float d = mx - mn;
    float h = 0.0;
    if (d > 0.0) {
        if (mx == c.r) {
            h = mod((c.g - c.b) / d, 6.0);
        } else if (mx == c.g) {
            h = (c.b - c.r) / d + 2.0;
        } else {
            h = (c.r - c.g) / d + 4.0;
        }
    }
    return vec3(h * 60.0, mx > 0.0 ? d / mx : 0.0, mx);
}

vec3 hsvToRgb(vec3 c) {
    vec3 k = mod(vec3(5.0, 3.0, 1.0) + c.x / 60.0, 6.0);
    return c.z - c.z * c.y * clamp(min(k, 4.0 - k), 0.0, 1.0);
}

// В пространство смешивания. Для полярных (OKLCH, HSV) оттенок — в градусах.
vec3 toSpace(vec3 c, int space) {
    if (space == 1) return toLinear(c);
    if (space == 2) return linearToOklab(toLinear(c));
    if (space == 3) {
        vec3 lab = linearToOklab(toLinear(c));
        float h = degrees(atan(lab.z, lab.y));
        return vec3(lab.x, length(lab.yz), h < 0.0 ? h + 360.0 : h);
    }
    if (space == 4) return rgbToHsv(c);
    return c;
}

vec3 fromSpace(vec3 c, int space) {
    if (space == 1) return toSrgb(c);
    if (space == 2) return toSrgb(oklabToLinear(c));
    if (space == 3) {
        float h = radians(c.z);
        return toSrgb(oklabToLinear(vec3(c.x, c.y * cos(h), c.y * sin(h))));
    }
    if (space == 4) return hsvToRgb(vec3(mod(c.x, 360.0), c.y, c.z));
    return c;
}

// Смешивание двух опор с предумноженной альфой (как ColorSpace.lerp в ядре).
vec4 mixStops(vec4 a, vec4 b, float t, int space) {
    float alpha = mix(a.a, b.a, t);
    if (alpha <= 0.0) {
        return vec4(0.0);
    }
    vec3 x = toSpace(a.rgb, space);
    vec3 y = toSpace(b.rgb, space);
    vec3 o;
    if (space == 3 || space == 4) {
        int hi = space == 3 ? 2 : 0;
        float chromaA = space == 3 ? x.y : x.y * x.z;
        float chromaB = space == 3 ? y.y : y.y * y.z;
        float ha = (chromaA < 1e-4 || a.a == 0.0) ? y[hi] : x[hi];
        float hb = (chromaB < 1e-4 || b.a == 0.0) ? x[hi] : y[hi];
        float dh = hb - ha;
        dh -= 360.0 * floor((dh + 180.0) / 360.0);
        o = (x * a.a + (y * b.a - x * a.a) * t) / alpha;
        o[hi] = ha + dh * t;
    } else {
        o = (x * a.a + (y * b.a - x * a.a) * t) / alpha;
    }
    return vec4(clamp(fromSpace(o, space), 0.0, 1.0), alpha);
}

vec4 gradient(vec2 p) {
    int kind = int(GradMode.x + 0.5);
    float t;
    if (kind == 1) {
        t = dot(p, GradGeom.xy) + GradGeom.z;
    } else if (kind == 2) {
        t = length((p - GradGeom.xy) * GradGeom.zw);
    } else {
        vec2 d = p - GradGeom.xy;
        t = fract((atan(d.x, -d.y) - GradGeom.z) / (2.0 * PI));
    }
    t += GradExtra.x;

    int n = int(GradMode.y + 0.5);
    vec4 c[8] = vec4[8](GradColorsA[0], GradColorsA[1], GradColorsA[2], GradColorsA[3],
                        GradColorsB[0], GradColorsB[1], GradColorsB[2], GradColorsB[3]);
    float s[8] = float[8](GradStopsA.x, GradStopsA.y, GradStopsA.z, GradStopsA.w,
                          GradStopsB.x, GradStopsB.y, GradStopsB.z, GradStopsB.w);
    if (GradMode.w > 0.5) {
        float first = s[0];
        float period = s[n - 1] - first;
        if (period > 1e-5) {
            t = first + mod(t - first, period);
        }
    }
    int space = int(GradMode.z + 0.5);
    if (t <= s[0]) {
        return c[0];
    }
    for (int i = 1; i < 8; i++) {
        if (i >= n) {
            break;
        }
        if (t <= s[i]) {
            float span = s[i] - s[i - 1];
            float f = span <= 1e-5 ? 1.0 : (t - s[i - 1]) / span;
            return mixStops(c[i - 1], c[i], f, space);
        }
    }
    return c[n - 1];
}

void main() {
    float d = roundedBoxSdf(localPos, HalfSize, Radii);
    if (Params.x > 0.0) {
        d = abs(d + Params.x * 0.5) - Params.x * 0.5;
    }
    float alpha;
    if (Params.y > 0.0) {
        alpha = 1.0 - smoothstep(-Params.y, Params.y, d);
    } else {
        // Сглаживание ровно в один физический пиксель при любом GUI scale.
        float aa = max(fwidth(d), 1e-4);
        alpha = clamp(0.5 - d / aa, 0.0, 1.0);
    }
    vec4 color = vertexColor * ColorModulator;
    if (GradMode.x > 0.5) {
        vec4 g = gradient(localPos);
        // Лёгкий шум против полос на плавных градиентах (±0.5 уровня из 255).
        float noise = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
        g.rgb = clamp(g.rgb + noise / 255.0, 0.0, 1.0);
        color *= g;
    }
    if (color.a * alpha < 0.002) {
        discard;
    }
    fragColor = vec4(color.rgb, color.a * alpha);
}
