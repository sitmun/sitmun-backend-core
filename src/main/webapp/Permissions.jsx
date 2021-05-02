import {Table} from "react-bootstrap";
import React from "react";

export function Permissions({permissions}) {
  function toString(cartographies) {
    return cartographies.map((it) => it.name).join(", ")
  }

  return (
    <Table>
      <thead>
      <tr>
        <th>Id</th>
        <th>Name</th>
        <th>Cartographies</th>
      </tr>
      </thead>
      <tbody>
      {permissions.sort((a, b) => a.id - b.id).map(permission =>
        <tr key={permission.id}>
          <td><code>{permission.id}</code></td>
          <td><code>{permission.name}</code></td>
          <td><code>{toString(permission.members)}</code></td>
        </tr>
      )}
      </tbody>
    </Table>
  );

}