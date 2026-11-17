using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ForestFire.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddNodeId : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "NodeId",
                table: "SensorReadings",
                type: "text",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "NodeId",
                table: "SensorReadings");
        }
    }
}
