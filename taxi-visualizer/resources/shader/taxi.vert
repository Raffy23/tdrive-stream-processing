#version 430 core
#extension GL_ARB_shader_draw_parameters : require

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec3 color;

layout(std430, binding = 2) buffer instancePosition { vec2 position[];        };

uniform mat4 projection;
out vec4 pColor;

void main()
{
    pColor = vec4(color, 1.0);
    gl_Position = projection * vec4(position[int(vertex.z) + gl_BaseInstanceARB + gl_InstanceID] + vertex.xy, 0.0, 1.0);
}