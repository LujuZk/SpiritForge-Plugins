import React from 'react';
import { useDroppable } from '@dnd-kit/core';
import { GRID_COLS, GRID_ROWS } from '../utils/constants';
import NodeCell from './NodeCell';
import ConnectorCell from './ConnectorCell';

const STATUS_STYLE = {
    'in-path': { border: '3px solid #00e5ff', box: '0 0 10px #00e5ff', bg: 'rgba(0,229,255,0.07)' },
    'hover-valid': { border: '3px solid #00ff88', box: '0 0 10px #00ff88', bg: 'rgba(0,255,136,0.08)' },
    'hover-invalid': { border: '2px solid #ff4444', box: '0 0 6px rgba(255,68,68,0.4)', bg: 'rgba(255,68,68,0.05)' },
};

function DroppableCell({ col, row, page, content, isSelected, isExclusive, shiftStatus, pathIndex,
    treeState,
    onClick, onPointerDown, onContextMenu, onMouseEnter }) {
    const id = `cell-${page},${col},${row}`;
    const { isOver, setNodeRef } = useDroppable({ id });
    const ss = STATUS_STYLE[shiftStatus] || null;

    const style = {
        width: 'var(--grid-cell-size)',
        height: 'var(--grid-cell-size)',
        border: ss?.border ?? (isSelected ? '2px solid var(--border-selected)' : isExclusive ? '2px solid #ff4444' : '1px solid var(--border-color)'),
        backgroundColor: isOver ? 'var(--cell-hover)' : (ss?.bg ?? (isExclusive ? 'rgba(255,68,68,0.06)' : 'transparent')),
        boxShadow: ss?.box ?? (isExclusive ? '0 0 8px rgba(255,68,68,0.35)' : 'none'),
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
        transition: 'border 0.1s, box-shadow 0.1s',
        cursor: shiftStatus === 'hover-valid' ? 'copy' : shiftStatus === 'hover-invalid' ? 'not-allowed' : undefined,
    };

    return (
        <div
            ref={setNodeRef}
            style={style}
            onClick={onClick}
            onPointerDown={onPointerDown} // Using bubble phase but with higher z-index / blocking
            onContextMenu={onContextMenu}
            onMouseEnter={onMouseEnter}
        >
            {shiftStatus === 'in-path' && pathIndex !== undefined && (
                <div style={{
                    position: 'absolute', top: '2px', left: '2px',
                    backgroundColor: '#00e5ff', color: '#000',
                    borderRadius: '50%', width: '16px', height: '16px',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '10px', fontWeight: 'bold', zIndex: 10, pointerEvents: 'none'
                }}>
                    {pathIndex + 1}
                </div>
            )}

            {/* When Shift is down, we show an invisible overlay to capture clicks before children (nodes/connectors) get them */}
            {shiftStatus && (
                <div style={{
                    position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                    zIndex: 20, cursor: 'pointer', backgroundColor: 'transparent'
                }} />
            )}

            {content?.type === 'node' && (
                <NodeCell
                    id={id}
                    content={content}
                    isSelected={isSelected}
                    isExclusive={isExclusive}
                    onClick={onClick}
                    onContextMenu={onContextMenu}
                    skillId={treeState.treeContext.id}
                />
            )}
            {content?.type === 'connector' && (
                <ConnectorCell id={id} content={content} isSelected={isSelected} onClick={onClick} onContextMenu={onContextMenu} />
            )}
        </div>
    );
}

function Grid({ treeState }) {
    const {
        treeContext, selectedCellFormat, setSelectedCellFormat, removeCell, addEdge,
        currentPage, totalPages, goToPage, getCellContent,
        shiftPath, paths, canAddToPath, addToShiftPath, finalizeCurrentPath, clearShiftPath
    } = treeState;

    const [isShiftDown, setIsShiftDown] = React.useState(false);
    const [hoverCell, setHoverCell] = React.useState(null);

    React.useEffect(() => {
        const onKeyDown = (e) => {
            if (e.key === 'Shift') {
                setIsShiftDown(true);
            }
            if (e.key === 'Escape') {
                clearShiftPath();
                setHoverCell(null);
            }
            if ((e.key === 'Delete' || e.key === 'Backspace') && selectedCellFormat) {
                const parts = selectedCellFormat.split(',');
                if (parts.length === 3) {
                    const [p, c, r] = parts.map(Number);
                    if (p === currentPage) removeCell(p, c, r);
                }
            }
        };
        const onKeyUp = (e) => {
            if (e.key === 'Shift') {
                setIsShiftDown(false);
                setHoverCell(null);
                // We DON'T auto-clear here anymore, let user finalize manually or it auto-finalizes on 2nd node
            }
        };
        window.addEventListener('keydown', onKeyDown);
        window.addEventListener('keyup', onKeyUp);
        return () => {
            window.removeEventListener('keydown', onKeyDown);
            window.removeEventListener('keyup', onKeyUp);
        };
    }, [selectedCellFormat, currentPage, clearShiftPath, removeCell]);

    const handleCellAction = (e, col, row, content) => {
        if (isShiftDown && e.button === 0) {
            e.preventDefault();
            e.stopPropagation();
            console.log(`[Grid] Shift-Click at ${col},${row}`);
            addToShiftPath(currentPage, col, row, content);
            return;
        }

        if (e.button !== 0) return;

        const cellKey = `${currentPage},${col},${row}`;
        if (content) {
            if (selectedCellFormat) {
                const parts = selectedCellFormat.split(',');
                if (parts.length === 3) {
                    const [selPage, selCol, selRow] = parts.map(Number);
                    const selContent = treeContext.cells[`${selPage},${selCol},${selRow}`];
                    if (selContent?.type === 'node' && content.type === 'node') {
                        addEdge(selContent.nodeId, content.nodeId);
                    }
                }
            }
        }
        setSelectedCellFormat(cellKey);
    };

    const handleMouseEnter = (col, row) => {
        if (isShiftDown) setHoverCell({ col, row });
    };

    const getShiftStatus = (col, row, content) => {
        const key = `${currentPage},${col},${row}`;
        if (shiftPath.some(p => p.key === key)) return 'in-path';

        const inSavedPath = (paths || []).some(p =>
            (p.cells || []).some(n => n.page === currentPage && n.col === col && n.row === row)
        );
        if (inSavedPath) return 'in-path';

        if (!isShiftDown) return null;
        if (!hoverCell || hoverCell.col !== col || hoverCell.row !== row) return null;

        return canAddToPath(currentPage, col, row, content) ? 'hover-valid' : 'hover-invalid';
    };

    // Compute exclusiveWith set of the currently selected node
    const selectedExclusiveSet = React.useMemo(() => {
        if (!selectedCellFormat) return new Set();
        const selCell = treeContext.cells[selectedCellFormat];
        if (!selCell || selCell.type !== 'node') return new Set();
        return new Set(selCell.exclusiveWith || []);
    }, [selectedCellFormat, treeContext.cells]);

    const matrix = [];
    for (let r = 0; r < GRID_ROWS; r++) {
        const colArray = [];
        for (let c = 0; c < GRID_COLS; c++) {
            const key = `${currentPage},${c},${r}`;
            const content = getCellContent(c, r);
            const shiftStatus = getShiftStatus(c, r, content);
            const pathIdx = shiftPath.findIndex(p => p.key === key);
            const isExclusive = content?.type === 'node' && selectedExclusiveSet.has(content.nodeId);

            colArray.push(
                <DroppableCell
                    key={key}
                    col={c} row={r} page={currentPage}
                    content={content}
                    isSelected={selectedCellFormat === key}
                    isExclusive={isExclusive}
                    shiftStatus={shiftStatus}
                    pathIndex={pathIdx >= 0 ? pathIdx : undefined}
                    treeState={treeState}
                    onPointerDown={(e) => handleCellAction(e, c, r, content)}
                    onContextMenu={(e) => { e.preventDefault(); setSelectedCellFormat(key); }}
                    onMouseEnter={() => handleMouseEnter(c, r)}
                />
            );
        }
        matrix.push(colArray);
    }

    const shiftNodeCount = shiftPath.filter(p => p.type === 'node').length;
    let helperText = null;
    if (isShiftDown) {
        if (shiftPath.length === 0) helperText = "Click en un NODO para empezar";
        else if (shiftNodeCount === 1) helperText = "Click en CONECTORES y terminá en un NODO";
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
            <div
                onMouseLeave={() => setHoverCell(null)}
                style={{
                    display: 'grid', gridTemplateRows: `repeat(${GRID_ROWS}, 1fr)`,
                    gap: '2px', padding: '24px', backgroundColor: '#0f0a05',
                    border: isShiftDown ? '4px solid #00e5ff' : '4px solid #3a2a18',
                    borderRadius: '8px', boxShadow: isShiftDown ? '0 0 30px rgba(0,229,255,0.2)' : '0 8px 32px rgba(0,0,0,0.5)',
                    position: 'relative'
                }}
            >
                {matrix.map((row, i) => (
                    <div key={`row-${i}`} style={{ display: 'flex', gap: '2px' }}>{row}</div>
                ))}
            </div>

            {isShiftDown && (
                <div style={{ color: '#00e5ff', fontSize: '14px', fontWeight: 'bold', background: 'rgba(0,0,0,0.6)', padding: '5px 15px', borderRadius: '15px' }}>
                    MODO SHIFT: {helperText} (ESC para cancelar)
                </div>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <button onClick={() => goToPage(currentPage - 1)} disabled={currentPage === 0}>←</button>
                <span>Página {currentPage + 1} de {totalPages}</span>
                <button onClick={() => goToPage(currentPage + 1)} disabled={currentPage >= totalPages - 1}>→</button>
            </div>
        </div>
    );
}

export default Grid;
