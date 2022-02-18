import {Table} from "react-bootstrap";
import React from "react";

export function Territory({territory}) {
  return (
    <Table>
      <thead>
      <tr>
        <th>Property</th>
        <th>Value</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td><code>id</code></td>
        <td><code>{territory.id}</code></td>
      </tr>
      <tr>
        <td><code>code</code></td>
        <td><code>{territory.code}</code></td>
      </tr>
      <tr>
        <td><code>name</code></td>
        <td><code>{territory.name}</code></td>
      </tr>
      <tr>
        <td><code>territorialAuthorityName</code></td>
        <td><code>{territory.territorialAuthorityName}</code></td>
      </tr>
      <tr>
        <td><code>territorialAuthorityAddress</code></td>
        <td><code>{territory.territorialAuthorityAddress}</code></td>
      </tr>
      <tr>
        <td><code>territorialAuthorityEmail</code></td>
        <td><code>{territory.territorialAuthorityEmail}</code></td>
      </tr>
      <tr>
        <td><code>territorialAuthorityLogo</code></td>
        <td><code>{territory.territorialAuthorityLogo}</code></td>
      </tr>
      <tr>
        <td><code>scope</code></td>
        <td><code>{territory.scope}</code></td>
      </tr>
      <tr>
        <td><code>extent</code></td>
        <td>
          <code>{territory.extent?.minX} {territory.extent?.minY} {territory.extent?.maxX} {territory.extent?.maxY}</code>
        </td>
      </tr>
      <tr>
        <td><code>type.name</code></td>
        <td><code>{territory.type?.name}</code></td>
      </tr>
      <tr>
        <td><code>groupType.name</code></td>
        <td><code>{territory.groupType?.name}</code></td>
      </tr>
      </tbody>
    </Table>
  );

}