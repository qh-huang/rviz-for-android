precision mediump float;
attribute float aChannel;
attribute float aX;
attribute float aY;
attribute float aZ;
uniform mat4 uMvp;
uniform int uColorMode; // 0 = channel, 1 = flat color
uniform vec4 uColor;
uniform float uMinVal;
uniform float uMaxVal;

varying vec4 vColor;

vec4 hToRGB(float h) {
	float hs = 4.0*h;
	float hi = floor(hs);
	float f = (hs) - floor(hs);
	float q = 1.0 - f;
	if (hi <= 0.0)
		return vec4(1.0, f, 0.0, 1.0);
	if (hi <= 1.0)
		return vec4(q, 1.0, 0.0, 1.0);
	if (hi <= 2.0)
		return vec4(0.0, 1.0, f, 1.0);
	if (hi <= 3.0)
		return vec4(0.0, q, 1.0, 1.0);
	if (hi <= 4.0)
		return vec4(f, 0.0, 1.0, 1.0);
	else
		return vec4(1.0, 0.0, q, 1.0);
}

vec4 hToGray(float h) {
	return mix(vec4(0.0, 0.0, 0.0, 1.0), vec4(1.0,1.0,1.0,1.0), h);
}

void main() {
	vec4 position = vec4(aX, aY, aZ, 1.0);
	gl_Position = uMvp * position;
	
	gl_PointSize = 3.0;
	
	if (uColorMode == 1) {
		vColor = uColor;
	} else {
		vColor = hToRGB((max(min(aChannel, uMaxVal),uMinVal)-uMinVal)/(uMaxVal-uMinVal));
	}
}
