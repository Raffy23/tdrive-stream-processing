#version 430 core
#extension GL_ARB_shader_draw_parameters : require

layout (location = 0) in vec3 vertex;

layout(std430, binding = 1) buffer instancePosition {
     vec2 position[];
};

layout(std430, binding = 2) buffer instanceColor {
     vec4 color[];
};

uniform mat4 projection;
uniform int colorID;
uniform int base;

out vec4 pColor;

void main()
{
    pColor = vec4(color[colorID].rgb, min(0.2, float(gl_InstanceID)/float(base)));
    gl_Position = projection * vec4(position[gl_BaseInstanceARB+gl_InstanceID] + vertex.xy, 0.0, 1.0);
}